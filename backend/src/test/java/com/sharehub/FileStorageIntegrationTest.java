package com.sharehub;

import com.sharehub.auth.RequestAccessService;
import com.sharehub.files.FileCategory;
import com.sharehub.files.FileRepository;
import com.sharehub.files.FileStorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FileStorageIntegrationTest {

    private static final String USER_KEY = "local-dev-user";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private FileRepository fileRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldUploadResourceAttachmentIntoDatabase() throws Exception {
        String body = """
            {
              "title": "数据库资料",
              "type": "PDF",
              "visibility": "PUBLIC"
            }
            """;

        MvcResult createResult = mockMvc.perform(post("/api/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").exists())
            .andReturn();

        JsonNode createJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long resourceId = createJson.get("data").get("id").asLong();

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "guide.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "hello pdf".getBytes()
        );

        MvcResult attachmentResult = mockMvc.perform(multipart("/api/resources/{id}/attachment", resourceId).file(file)
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.file.filename").value("guide.pdf"))
            .andExpect(jsonPath("$.data.file.downloadUrl").exists())
            .andReturn();

        JsonNode json = objectMapper.readTree(attachmentResult.getResponse().getContentAsString());
        String downloadUrl = json.get("data").get("file").get("downloadUrl").asText();

        MvcResult downloadResult = mockMvc.perform(get(downloadUrl))
            .andExpect(status().isOk())
            .andReturn();

        assertThat(downloadResult.getResponse().getContentAsByteArray()).isEqualTo("hello pdf".getBytes());
    }

    @Test
    void shouldRejectTooLargeAvatarUpload() throws Exception {
        byte[] payload = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "avatar.png",
            MediaType.IMAGE_PNG_VALUE,
            payload
        );

        mockMvc.perform(multipart("/api/auth/avatar")
                .file(file)
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnNotFoundWhenFileMissing() throws Exception {
        UUID missingId = UUID.randomUUID();

        mockMvc.perform(get("/api/files/{id}", missingId))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldUploadFileDirectlyWithoutLoginAndDownload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "resume.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "resume body".getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/files/upload")
                .file(file)
                .param("owner", "user-direct")
                .param("category", "RESUME_PDF")
                .param("referenceType", "RESUME")
                .param("referenceId", "resume-100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.owner").value("user-direct"))
            .andExpect(jsonPath("$.data.filename").value("resume.pdf"))
            .andExpect(jsonPath("$.data.downloadUrl").exists())
            .andReturn();

        JsonNode uploadJson = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        String downloadUrl = uploadJson.get("data").get("downloadUrl").asText();

        MvcResult downloadResult = mockMvc.perform(get(downloadUrl))
            .andExpect(status().isOk())
            .andReturn();

        assertThat(downloadResult.getResponse().getContentAsByteArray()).isEqualTo("resume body".getBytes());
    }

    @Test
    void shouldDownloadDirectUploadWithAttachmentHeaderAndDefaultContentType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "resume.bin",
            null,
            "resume body".getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/files/upload")
                .file(file)
                .param("owner", "user-direct")
                .param("category", "RESUME_PDF")
                .param("referenceType", "RESUME")
                .param("referenceId", "resume-101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.contentType").value(MediaType.APPLICATION_OCTET_STREAM_VALUE))
            .andReturn();

        JsonNode uploadJson = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        String downloadUrl = uploadJson.get("data").get("downloadUrl").asText();

        mockMvc.perform(get(downloadUrl))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"resume.bin\""));
    }

    @Test
    void shouldFallbackDirectUploadContentTypeWhenUploadContentTypeBlank() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "resume-blank.bin",
            " ",
            "resume body".getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/files/upload")
                .file(file)
                .param("owner", "user-direct")
                .param("category", "RESUME_PDF")
                .param("referenceType", "RESUME")
                .param("referenceId", "resume-101-blank"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.contentType").value(MediaType.APPLICATION_OCTET_STREAM_VALUE))
            .andReturn();

        JsonNode uploadJson = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        String downloadUrl = uploadJson.get("data").get("downloadUrl").asText();

        mockMvc.perform(get(downloadUrl))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"resume-blank.bin\""));
    }

    @Test
    void shouldFallbackDirectUploadContentTypeWhenUploadContentTypeInvalid() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "resume-invalid.bin",
            "invalid/type;",
            "resume body".getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/files/upload")
                .file(file)
                .param("owner", "user-direct")
                .param("category", "RESUME_PDF")
                .param("referenceType", "RESUME")
                .param("referenceId", "resume-101-invalid"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.contentType").value(MediaType.APPLICATION_OCTET_STREAM_VALUE))
            .andReturn();

        JsonNode uploadJson = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        String downloadUrl = uploadJson.get("data").get("downloadUrl").asText();

        mockMvc.perform(get(downloadUrl))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"resume-invalid.bin\""));
    }

    @Test
    void shouldRejectDirectUploadWhenOwnerMissing() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "avatar.png",
            MediaType.IMAGE_PNG_VALUE,
            new byte[]{1}
        );

        mockMvc.perform(multipart("/api/files/upload")
                .file(file)
                .param("owner", " ")
                .param("category", "AVATAR")
                .param("referenceType", "USER")
                .param("referenceId", "user-1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("FILE_OWNER_REQUIRED"));
    }

    @Test
    void shouldRejectDirectUploadWhenReferenceMissing() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "avatar.png",
            MediaType.IMAGE_PNG_VALUE,
            new byte[]{1}
        );

        mockMvc.perform(multipart("/api/files/upload")
                .file(file)
                .param("owner", "user-1")
                .param("category", "AVATAR")
                .param("referenceType", " ")
                .param("referenceId", " "))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("FILE_REFERENCE_REQUIRED"));
    }

    @Test
    void shouldRejectDirectUploadWhenFileEmpty() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "avatar.png",
            MediaType.IMAGE_PNG_VALUE,
            new byte[0]
        );

        mockMvc.perform(multipart("/api/files/upload")
                .file(file)
                .param("owner", "user-1")
                .param("category", "AVATAR")
                .param("referenceType", "USER")
                .param("referenceId", "user-1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("FILE_EMPTY"));
    }

    @Test
    void shouldRejectDirectUploadWhenFileTooLarge() throws Exception {
        byte[] payload = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "resume.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            payload
        );

        mockMvc.perform(multipart("/api/files/upload")
                .file(file)
                .param("owner", "user-1")
                .param("category", "RESUME_PDF")
                .param("referenceType", "RESUME")
                .param("referenceId", "resume-102"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("FILE_TOO_LARGE"));
    }

    @Test
    void shouldRejectDirectUploadWhenFilenameMissing() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            " ",
            MediaType.APPLICATION_OCTET_STREAM_VALUE,
            new byte[]{1}
        );

        mockMvc.perform(multipart("/api/files/upload")
                .file(file)
                .param("owner", "user-1")
                .param("category", "AVATAR")
                .param("referenceType", "USER")
                .param("referenceId", "user-1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("FILE_NAME_REQUIRED"));
    }

    @Test
    void shouldPreserveExplicitContentTypeOnDownload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "notes.txt",
            MediaType.TEXT_PLAIN_VALUE,
            "hello text".getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/files/upload")
                .file(file)
                .param("owner", "user-plain")
                .param("category", "RESOURCE_ATTACHMENT")
                .param("referenceType", "RESOURCE")
                .param("referenceId", "resource-plain"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.contentType").value(MediaType.TEXT_PLAIN_VALUE))
            .andReturn();

        JsonNode uploadJson = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        String downloadUrl = uploadJson.get("data").get("downloadUrl").asText();

        mockMvc.perform(get(downloadUrl))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", MediaType.TEXT_PLAIN_VALUE));
    }

    @Test
    void shouldFallbackToOctetStreamWhenStoredContentTypeInvalid() throws Exception {
        var stored = fileStorageService.storeBytes(
            "user-invalid",
            FileCategory.RESOURCE_ATTACHMENT,
            "RESOURCE",
            "resource-invalid",
            "broken.bin",
            "application/json",
            "broken".getBytes()
        );

        var record = fileRepository.findById(stored.id()).orElseThrow();
        record.setContentType("invalid/type;");
        fileRepository.save(record);

        mockMvc.perform(get(stored.downloadUrl()))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"broken.bin\""));
    }

    @Test
    void shouldFallbackToOctetStreamWhenStoredContentTypeNull() throws Exception {
        var stored = fileStorageService.storeBytes(
            "user-null",
            FileCategory.RESOURCE_ATTACHMENT,
            "RESOURCE",
            "resource-null",
            "null.bin",
            MediaType.APPLICATION_JSON_VALUE,
            "null-body".getBytes()
        );

        var record = fileRepository.findById(stored.id()).orElseThrow();
        record.setContentType(null);
        fileRepository.save(record);

        mockMvc.perform(get(stored.downloadUrl()))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"null.bin\""));
    }
}
