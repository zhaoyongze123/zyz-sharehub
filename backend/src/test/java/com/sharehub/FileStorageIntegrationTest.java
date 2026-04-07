package com.sharehub;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FileStorageIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldUploadResourceAttachmentIntoDatabase() throws Exception {
        String body = """
            {
              "title": "数据库资料",
              "type": "PDF",
              "visibility": "PUBLIC"
            }
            """;

        mockMvc.perform(post("/api/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").exists());

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "guide.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "hello pdf".getBytes()
        );

        mockMvc.perform(multipart("/api/resources/{id}/attachment", 1L).file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.file.filename").value("guide.pdf"))
            .andExpect(jsonPath("$.data.file.downloadUrl").exists());
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

        mockMvc.perform(multipart("/api/auth/avatar").file(file))
            .andExpect(status().isBadRequest());
    }
}
