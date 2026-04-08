package com.sharehub.resource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sharehub.auth.UserProfileDto;
import com.sharehub.auth.UserProfileRepository;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResourceControllerIntegrationTest {
    private static final String USER_KEY_HEADER = "X-User-Key";
    private static final String DEFAULT_USER = "local-dev-user";
    private static final String OTHER_USER = "other-user";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM likes");
        jdbcTemplate.update("DELETE FROM favorites");
        jdbcTemplate.update("DELETE FROM comments");
        jdbcTemplate.update("DELETE FROM reports");
        jdbcTemplate.update("DELETE FROM resources");
        jdbcTemplate.update("DELETE FROM files");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void shouldCreateDraftResourceWithExtendedFields() throws Exception {
        mockMvc.perform(post("/api/resources")
                .header(USER_KEY_HEADER, DEFAULT_USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(resourceBody("Spring 实战", "PDF", "PUBLIC", "spring,java", "资源简介")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("Spring 实战"))
            .andExpect(jsonPath("$.data.category").value("PDF"))
            .andExpect(jsonPath("$.data.status").value("DRAFT"))
            .andExpect(jsonPath("$.data.visibility").value("PUBLIC"))
            .andExpect(jsonPath("$.data.likes").value(0))
            .andExpect(jsonPath("$.data.favorites").value(0))
            .andExpect(jsonPath("$.data.downloadCount").value(0));
    }

    @Test
    void shouldFilterListByDefaultStatusCategoryTagAndHotSort() throws Exception {
        long publishedSpring = createResource("Spring 指南", "PDF", "PUBLIC", "spring,java", "最热资料");
        long draftPrivate = createResource("私有草稿", "DOC", "PRIVATE", "draft", "草稿资料");
        long publishedAi = createResource("AI 清单", "DOC", "PUBLIC", "ai,rag", "第二资料");

        publishResource(publishedSpring);
        publishResource(publishedAi);
        likeResource(publishedSpring);
        likeResource(publishedSpring);
        favoriteResource(publishedSpring);

        mockMvc.perform(get("/api/resources"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(2))
            .andExpect(jsonPath("$.data.items.length()").value(2));

        mockMvc.perform(get("/api/resources")
                .param("status", "PUBLISHED,DRAFT")
                .param("visibility", "PRIVATE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].title").value("私有草稿"));

        mockMvc.perform(get("/api/resources")
                .param("category", "PDF")
                .param("tag", "spring")
                .param("sortBy", "hot"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].id").value(publishedSpring))
            .andExpect(jsonPath("$.data.items[0].likes").value(1))
            .andExpect(jsonPath("$.data.items[0].favorites").value(1));

        mockMvc.perform(get("/api/resources")
                .param("keyword", "AI"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].title").value("AI 清单"));

        resourceRepository.findById(draftPrivate).ifPresent(entity -> {});
    }

    @Test
    void shouldTreatBlankFilterParamsAsUnset() throws Exception {
        long publishedSpring = createResource("Spring 指南", "PDF", "PUBLIC", "spring,java", "最热资料");
        createResource("私有草稿", "DOC", "PRIVATE", "draft", "草稿资料");
        publishResource(publishedSpring);

        mockMvc.perform(get("/api/resources")
                .param("keyword", " ")
                .param("category", "")
                .param("tag", " ")
                .param("visibility", ""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].id").value(publishedSpring))
            .andExpect(jsonPath("$.data.items[0].title").value("Spring 指南"));
    }

    @Test
    void shouldDifferentiateDetailStates() throws Exception {
        long normalId = createResource("正常资料", "PDF", "PUBLIC", "java", "正常");
        long removedId = createResource("下架资料", "PDF", "PUBLIC", "java", "下架");
        publishResource(normalId);
        publishResource(removedId);
        ResourceEntity removed = resourceRepository.findById(removedId).orElseThrow();
        removed.setStatus("REMOVED");
        resourceRepository.save(removed);

        mockMvc.perform(get("/api/resources/{id}", normalId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(normalId))
            .andExpect(jsonPath("$.data.updatedAt").exists())
            .andExpect(jsonPath("$.data.author").value("local-dev-user"));

        mockMvc.perform(get("/api/resources/{id}", 999999))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        mockMvc.perform(get("/api/resources/{id}", removedId))
            .andExpect(status().isGone())
            .andExpect(jsonPath("$.code").value("RESOURCE_REMOVED"));
    }

    @Test
    void shouldUpdateAndPublishResource() throws Exception {
        long resourceId = createResource("旧标题", "PDF", "PUBLIC", "java", "旧简介");

        mockMvc.perform(put("/api/resources/{id}", resourceId)
                .header(USER_KEY_HEADER, DEFAULT_USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "新标题",
                      "category": "DOC",
                      "summary": "新简介",
                      "tags": ["spring", "guide"],
                      "visibility": "PRIVATE",
                      "status": "DRAFT"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("新标题"))
            .andExpect(jsonPath("$.data.category").value("DOC"))
            .andExpect(jsonPath("$.data.visibility").value("PRIVATE"));

        mockMvc.perform(post("/api/resources/{id}/publish", resourceId)
                .header(USER_KEY_HEADER, DEFAULT_USER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        mockMvc.perform(get("/api/resources/{id}", resourceId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("新标题"))
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    void shouldRejectNonOwnerMutationRequests() throws Exception {
        long resourceId = createResource("多用户资源", "PDF", "PUBLIC", "java", "隔离");
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "guide.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "pdf-content".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(put("/api/resources/{id}", resourceId)
                .header(USER_KEY_HEADER, OTHER_USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(resourceBody("越权标题", "DOC", "PRIVATE", "hack", "越权")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("RESOURCE_FORBIDDEN"));

        mockMvc.perform(post("/api/resources/{id}/publish", resourceId)
                .header(USER_KEY_HEADER, OTHER_USER))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("RESOURCE_FORBIDDEN"));

        mockMvc.perform(multipart("/api/resources/{id}/attachment", resourceId)
                .file(file)
                .header(USER_KEY_HEADER, OTHER_USER))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("RESOURCE_FORBIDDEN"));

        mockMvc.perform(delete("/api/resources/{id}", resourceId)
                .header(USER_KEY_HEADER, OTHER_USER))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("RESOURCE_FORBIDDEN"));

        mockMvc.perform(get("/api/resources/{id}", resourceId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("多用户资源"));
    }

    @Test
    void shouldReturnRelatedResources() throws Exception {
        long sourceId = createResource("Spring Core", "PDF", "PUBLIC", "spring,java", "source");
        long sameTypeId = createResource("Spring MVC", "PDF", "PUBLIC", "mvc", "same type");
        long sameTagId = createResource("Java 集合", "DOC", "PUBLIC", "java,collection", "same tag");
        long unrelatedId = createResource("Redis 入门", "ZIP", "PUBLIC", "redis", "unrelated");
        long draftId = createResource("草稿相关", "PDF", "PUBLIC", "spring", "draft");

        publishResource(sourceId);
        publishResource(sameTypeId);
        publishResource(sameTagId);
        publishResource(unrelatedId);

        mockMvc.perform(get("/api/resources/{id}/related", sourceId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].id").value(sameTypeId))
            .andExpect(jsonPath("$.data[1].id").value(sameTagId));
    }

    @Test
    void shouldDeleteWithStableSemantics() throws Exception {
        long resourceId = createResource("待删除", "PDF", "PUBLIC", "java", "删除");

        mockMvc.perform(delete("/api/resources/{id}", resourceId)
                .header(USER_KEY_HEADER, DEFAULT_USER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("DELETED"));

        mockMvc.perform(get("/api/resources/{id}", resourceId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        mockMvc.perform(delete("/api/resources/{id}", 999999)
                .header(USER_KEY_HEADER, DEFAULT_USER))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void shouldUploadAttachmentAndWriteBackObjectKey() throws Exception {
        long resourceId = createResource("附件资源", "PDF", "PUBLIC", "java", "附件");
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "guide.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "pdf-content".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/resources/{id}/attachment", resourceId)
                .file(file)
                .header(USER_KEY_HEADER, DEFAULT_USER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.file.owner").value(DEFAULT_USER))
            .andExpect(jsonPath("$.data.resourceId").value(resourceId))
            .andExpect(jsonPath("$.data.file.filename").value("guide.pdf"))
            .andExpect(jsonPath("$.data.file.contentType").value(MediaType.APPLICATION_PDF_VALUE))
            .andExpect(jsonPath("$.data.file.downloadUrl").exists());

        MockMultipartFile fallbackContentTypeFile = new MockMultipartFile(
            "file",
            "guide-no-type.pdf",
            null,
            "pdf-content".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/resources/{id}/attachment", resourceId)
                .file(fallbackContentTypeFile)
                .header(USER_KEY_HEADER, DEFAULT_USER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.resourceId").value(resourceId))
            .andExpect(jsonPath("$.data.file.filename").value("guide-no-type.pdf"))
            .andExpect(jsonPath("$.data.file.contentType").value(MediaType.APPLICATION_OCTET_STREAM_VALUE))
            .andExpect(jsonPath("$.data.file.downloadUrl").exists());

        MockMultipartFile invalidContentTypeFile = new MockMultipartFile(
            "file",
            "guide-invalid-type.pdf",
            "invalid/type;",
            "pdf-content".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/resources/{id}/attachment", resourceId)
                .file(invalidContentTypeFile)
                .header(USER_KEY_HEADER, DEFAULT_USER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.resourceId").value(resourceId))
            .andExpect(jsonPath("$.data.file.filename").value("guide-invalid-type.pdf"))
            .andExpect(jsonPath("$.data.file.contentType").value(MediaType.APPLICATION_OCTET_STREAM_VALUE))
            .andExpect(jsonPath("$.data.file.downloadUrl").exists());

        mockMvc.perform(get("/api/resources/{id}", resourceId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.objectKey").isNotEmpty())
            .andExpect(jsonPath("$.data.downloadCount").value(1));

        mockMvc.perform(multipart("/api/resources/{id}/attachment", 999999L)
                .file(file)
                .header(USER_KEY_HEADER, DEFAULT_USER))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void shouldRejectInvalidResourceAttachmentFile() throws Exception {
        long resourceId = createResource("附件校验资源", "PDF", "PUBLIC", "java", "附件校验");

        MockMultipartFile emptyFile = new MockMultipartFile(
            "file",
            "empty.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            new byte[0]
        );

        mockMvc.perform(multipart("/api/resources/{id}/attachment", resourceId)
                .file(emptyFile)
                .header(USER_KEY_HEADER, DEFAULT_USER))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("FILE_EMPTY"));

        byte[] oversizedPayload = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile oversizedFile = new MockMultipartFile(
            "file",
            "oversized.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            oversizedPayload
        );

        mockMvc.perform(multipart("/api/resources/{id}/attachment", resourceId)
                .file(oversizedFile)
                .header(USER_KEY_HEADER, DEFAULT_USER))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("FILE_TOO_LARGE"));
    }

    @Test
    void shouldRequireUserForResourceMutationEndpoints() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "guide.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "pdf-content".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(post("/api/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .content(resourceBody("未登录资源", "PDF", "PUBLIC", "java", "附件")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NOT_LOGGED_IN"));

        mockMvc.perform(put("/api/resources/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(resourceBody("未登录更新", "DOC", "PRIVATE", "spring", "更新")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NOT_LOGGED_IN"));

        mockMvc.perform(delete("/api/resources/{id}", 1L))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NOT_LOGGED_IN"));

        mockMvc.perform(post("/api/resources/{id}/publish", 1L))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NOT_LOGGED_IN"));

        mockMvc.perform(multipart("/api/resources/{id}/attachment", 1L)
                .file(file))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NOT_LOGGED_IN"));
    }

    @Test
    void shouldRejectBannedUserForResourceMutationEndpoints() throws Exception {
        UserProfileDto bannedUser = userProfileRepository.upsert("resource-banned-user", "resource-banned-user", null);
        userProfileRepository.updateStatus(bannedUser.id(), "BANNED");
        long resourceId = createResource("封禁态资源", "PDF", "PUBLIC", "java", "封禁校验");
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "guide.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "pdf-content".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(post("/api/resources")
                .header(USER_KEY_HEADER, bannedUser.login())
                .contentType(MediaType.APPLICATION_JSON)
                .content(resourceBody("封禁创建", "PDF", "PUBLIC", "java", "创建")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("USER_BANNED"));

        mockMvc.perform(put("/api/resources/{id}", resourceId)
                .header(USER_KEY_HEADER, bannedUser.login())
                .contentType(MediaType.APPLICATION_JSON)
                .content(resourceBody("封禁更新", "DOC", "PRIVATE", "spring", "更新")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("USER_BANNED"));

        mockMvc.perform(delete("/api/resources/{id}", resourceId)
                .header(USER_KEY_HEADER, bannedUser.login()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("USER_BANNED"));

        mockMvc.perform(post("/api/resources/{id}/publish", resourceId)
                .header(USER_KEY_HEADER, bannedUser.login()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("USER_BANNED"));

        mockMvc.perform(multipart("/api/resources/{id}/attachment", resourceId)
                .file(file)
                .header(USER_KEY_HEADER, bannedUser.login()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("USER_BANNED"));
    }

    private long createResource(String title, String category, String visibility, String tags, String summary) throws Exception {
        String response = mockMvc.perform(post("/api/resources")
                .header(USER_KEY_HEADER, DEFAULT_USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(resourceBody(title, category, visibility, tags, summary)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        Map<String, Object> payload = objectMapper.readValue(response, new TypeReference<>() {});
        Map<String, Object> data = (Map<String, Object>) payload.get("data");
        return Long.parseLong(String.valueOf(data.get("id")));
    }

    private void publishResource(long id) throws Exception {
        mockMvc.perform(post("/api/resources/{id}/publish", id)
                .header(USER_KEY_HEADER, DEFAULT_USER))
            .andExpect(status().isOk());
    }

    private void likeResource(long id) throws Exception {
        mockMvc.perform(post("/api/resources/{id}/like", id)
                .header(USER_KEY_HEADER, DEFAULT_USER))
            .andExpect(status().isOk());
    }

    private void favoriteResource(long id) throws Exception {
        mockMvc.perform(post("/api/resources/{id}/favorite", id)
                .header(USER_KEY_HEADER, DEFAULT_USER))
            .andExpect(status().isOk());
    }

    private String resourceBody(String title, String category, String visibility, String tags, String summary) {
        return """
            {
              "title": "%s",
              "category": "%s",
              "summary": "%s",
              "tags": [%s],
              "visibility": "%s"
            }
            """.formatted(
            title,
            category,
            summary,
            quoteTags(tags),
            visibility
        );
    }

    private String quoteTags(String tags) {
        return java.util.Arrays.stream(tags.split(","))
            .map(String::trim)
            .filter(item -> !item.isEmpty())
            .map(item -> "\"" + item + "\"")
            .reduce((left, right) -> left + ", " + right)
            .orElse("");
    }
}
