package com.sharehub.interaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sharehub.auth.RequestAccessService;
import com.sharehub.auth.UserProfileDto;
import com.sharehub.auth.UserProfileRepository;
import com.sharehub.config.AdminTokenFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class InteractionControllerIntegrationTest {

    private static final String USER_KEY = "local-dev-user";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM admin_audit_logs");
        jdbcTemplate.update("DELETE FROM reports");
        jdbcTemplate.update("DELETE FROM comments");
        jdbcTemplate.update("DELETE FROM favorites");
        jdbcTemplate.update("DELETE FROM likes");
        jdbcTemplate.update("DELETE FROM note_view_history");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void commentReplyFavoriteLikeReportPersisted() throws Exception {
        long resourceId = createResource("互动资源");
        var commentPayload = mapper.writeValueAsString(Map.of("content", "hello"));
        String commentResponse = mvc.perform(post("/api/resources/" + resourceId + "/comments")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(commentPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.resourceId").value(resourceId))
            .andReturn()
            .getResponse()
            .getContentAsString();

        long commentId = Long.parseLong(String.valueOf(((Map<?, ?>) mapper.readValue(commentResponse, Map.class).get("data")).get("id")));

        var replyPayload = mapper.writeValueAsString(Map.of("content", "reply"));
        mvc.perform(post("/api/comments/" + commentId + "/reply")
                        .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(replyPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parentId").value(commentId))
                .andExpect(jsonPath("$.data.resourceId").value(resourceId));

        mvc.perform(post("/api/resources/" + resourceId + "/favorite")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.favorites").value(1));

        mvc.perform(post("/api/resources/" + resourceId + "/like")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.likes").value(1));

        var reportPayload = mapper.writeValueAsString(Map.of("resourceId", String.valueOf(resourceId), "reason", "spam"));
        mvc.perform(post("/api/reports")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reportPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("OPEN"))
            .andExpect(jsonPath("$.data.targetId").value(resourceId));

        mvc.perform(get("/api/resources/" + resourceId + "/comments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].children", hasSize(1)));

        mvc.perform(get("/api/resources/" + resourceId + "/interactions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.resourceId").value(resourceId))
            .andExpect(jsonPath("$.data.comments").value(2))
            .andExpect(jsonPath("$.data.favorites").value(1))
            .andExpect(jsonPath("$.data.likes").value(1))
            .andExpect(jsonPath("$.data.reports").value(1));

        mvc.perform(post("/api/resources/" + resourceId + "/favorite")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.favorites").value(1));

        mvc.perform(delete("/api/resources/" + resourceId + "/favorite")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.favorites").value(0));

        mvc.perform(delete("/api/resources/" + resourceId + "/favorite")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.favorites").value(0));

        mvc.perform(post("/api/resources/" + resourceId + "/like")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.likes").value(1));

        mvc.perform(delete("/api/resources/" + resourceId + "/like")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.likes").value(0));

        mvc.perform(delete("/api/resources/" + resourceId + "/like")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.likes").value(0));

        mvc.perform(get("/api/resources/" + resourceId + "/interactions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.favorites").value(0))
            .andExpect(jsonPath("$.data.likes").value(0));

        long replyId = commentId + 1;

        mvc.perform(adminPost("/api/admin/comments/" + replyId + "/hide"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("HIDDEN"));

        mvc.perform(get("/api/resources/" + resourceId + "/comments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].children", hasSize(0)));

        mvc.perform(get("/api/resources/" + resourceId + "/interactions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.comments").value(1));

        mvc.perform(adminPost("/api/admin/comments/" + replyId + "/restore"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("VISIBLE"));

        mvc.perform(get("/api/resources/" + resourceId + "/comments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].children", hasSize(1)));

        mvc.perform(get("/api/resources/" + resourceId + "/interactions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.comments").value(2));
    }

    @Test
    void writeEndpointsShouldRequireLogin() throws Exception {
        var payload = mapper.writeValueAsString(Map.of("content", "hello"));

        mvc.perform(post("/api/resources/1/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NOT_LOGGED_IN"));

        mvc.perform(post("/api/comments/1/reply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NOT_LOGGED_IN"));

        mvc.perform(post("/api/resources/1/favorite"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NOT_LOGGED_IN"));

        mvc.perform(delete("/api/resources/1/favorite"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NOT_LOGGED_IN"));

        mvc.perform(post("/api/resources/1/like"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NOT_LOGGED_IN"));

        mvc.perform(delete("/api/resources/1/like"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NOT_LOGGED_IN"));

        mvc.perform(post("/api/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("resourceId", "1", "reason", "spam"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NOT_LOGGED_IN"));
    }

    @Test
    void writeEndpointsShouldRejectMissingResource() throws Exception {
        var payload = mapper.writeValueAsString(Map.of("content", "hello"));

        mvc.perform(post("/api/comments/999999/reply")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));

        mvc.perform(post("/api/resources/999999/comments")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        mvc.perform(post("/api/resources/999999/favorite")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        mvc.perform(delete("/api/resources/999999/favorite")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        mvc.perform(post("/api/resources/999999/like")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        mvc.perform(delete("/api/resources/999999/like")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        mvc.perform(post("/api/reports")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("resourceId", "999999", "reason", "spam"))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void writeEndpointsShouldRejectBannedUser() throws Exception {
        UserProfileDto banned = userProfileRepository.upsert("banned-user", "banned-user", null);
        userProfileRepository.updateStatus(banned.id(), "BANNED");

        var payload = mapper.writeValueAsString(Map.of("content", "hello"));

        mvc.perform(post("/api/resources/1/comments")
                .header(RequestAccessService.USER_KEY_HEADER, banned.login())
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("USER_BANNED"));

        mvc.perform(post("/api/comments/1/reply")
                .header(RequestAccessService.USER_KEY_HEADER, banned.login())
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("USER_BANNED"));

        mvc.perform(post("/api/resources/1/favorite")
                .header(RequestAccessService.USER_KEY_HEADER, banned.login()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("USER_BANNED"));

        mvc.perform(delete("/api/resources/1/favorite")
                .header(RequestAccessService.USER_KEY_HEADER, banned.login()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("USER_BANNED"));

        mvc.perform(post("/api/resources/1/like")
                .header(RequestAccessService.USER_KEY_HEADER, banned.login()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("USER_BANNED"));

        mvc.perform(delete("/api/resources/1/like")
                .header(RequestAccessService.USER_KEY_HEADER, banned.login()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("USER_BANNED"));

        mvc.perform(post("/api/reports")
                .header(RequestAccessService.USER_KEY_HEADER, banned.login())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("resourceId", "1", "reason", "spam"))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("USER_BANNED"));
    }

    @Test
    void commentEndpointsShouldRejectBlankContent() throws Exception {
        long resourceId = createResource("空评论校验资源");

        mvc.perform(post("/api/resources/" + resourceId + "/comments")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("content", " "))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("COMMENT_CONTENT_REQUIRED"));

        jdbcTemplate.update(
            """
                INSERT INTO comments (resource_id, note_id, parent_id, author_key, content, status, created_at)
                VALUES (?, NULL, NULL, ?, ?, 'VISIBLE', CURRENT_TIMESTAMP)
                """,
            resourceId,
            USER_KEY,
            "root"
        );

        mvc.perform(post("/api/comments/1/reply")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("content", ""))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("COMMENT_CONTENT_REQUIRED"));
    }

    @Test
    void reportEndpointShouldFallbackBlankReasonToDefault() throws Exception {
        long resourceId = createResource("空举报原因资源");

        mvc.perform(post("/api/reports")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("resourceId", String.valueOf(resourceId), "reason", "   "))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.targetId").value(resourceId))
            .andExpect(jsonPath("$.data.reason").value("无"));
    }

    @Test
    void reportEndpointShouldSupportNoteTarget() throws Exception {
        long noteId = createNote("笔记举报校验");

        mvc.perform(post("/api/reports")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "targetType", "NOTE",
                    "noteId", String.valueOf(noteId),
                    "reason", "note spam"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.targetType").value("NOTE"))
            .andExpect(jsonPath("$.data.targetId").value(noteId))
            .andExpect(jsonPath("$.data.reason").value("note spam"));
    }

    @Test
    void noteFavoriteAndLikeShouldPersisted() throws Exception {
        long noteId = createNote("互动笔记");

        mvc.perform(post("/api/notes/" + noteId + "/favorite")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.noteId").value(noteId))
            .andExpect(jsonPath("$.data.favorites").value(1));

        mvc.perform(post("/api/notes/" + noteId + "/favorite")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.favorites").value(1));

        mvc.perform(post("/api/notes/" + noteId + "/like")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.noteId").value(noteId))
            .andExpect(jsonPath("$.data.likes").value(1));

        mvc.perform(get("/api/notes/" + noteId + "/interactions")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.noteId").value(noteId))
            .andExpect(jsonPath("$.data.favorites").value(1))
            .andExpect(jsonPath("$.data.likes").value(1))
            .andExpect(jsonPath("$.data.reports").value(0));

        mvc.perform(delete("/api/notes/" + noteId + "/favorite")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.favorites").value(0));

        mvc.perform(delete("/api/notes/" + noteId + "/like")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.likes").value(0));
    }

    @Test
    void noteInteractionsShouldRespectAccessibility() throws Exception {
        long privateNoteId = createNote("私有互动笔记");
        long publicNoteId = createPublishedPublicNote("公开互动笔记");

        mvc.perform(get("/api/notes/" + privateNoteId + "/interactions"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOTE_NOT_FOUND"));

        mvc.perform(get("/api/notes/" + privateNoteId + "/interactions")
                .header(RequestAccessService.USER_KEY_HEADER, "other-reader"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOTE_NOT_FOUND"));

        mvc.perform(get("/api/notes/" + privateNoteId + "/interactions")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.noteId").value(privateNoteId));

        mvc.perform(get("/api/notes/" + publicNoteId + "/interactions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.noteId").value(publicNoteId));
    }

    @Test
    void noteWriteInteractionsShouldRejectInaccessibleNote() throws Exception {
        long privateNoteId = createNote("不可见笔记");

        mvc.perform(post("/api/notes/" + privateNoteId + "/favorite")
                .header(RequestAccessService.USER_KEY_HEADER, "other-reader"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOTE_NOT_FOUND"));

        mvc.perform(post("/api/notes/" + privateNoteId + "/like")
                .header(RequestAccessService.USER_KEY_HEADER, "other-reader"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOTE_NOT_FOUND"));

        mvc.perform(post("/api/reports")
                .header(RequestAccessService.USER_KEY_HEADER, "other-reader")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "targetType", "NOTE",
                    "noteId", String.valueOf(privateNoteId),
                    "reason", "inaccessible"
                ))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOTE_NOT_FOUND"));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder adminPost(String uri) {
        return post(uri).header(AdminTokenFilter.HEADER, AdminTokenFilter.DEFAULT_ADMIN_TOKEN);
    }

    private long createResource(String title) {
        jdbcTemplate.update(
            """
                INSERT INTO resources (title, type, summary, owner_key, visibility, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
            title,
            "PDF",
            "互动测试资源",
            USER_KEY,
            "PUBLIC",
            "PUBLISHED"
        );
        Long id = jdbcTemplate.queryForObject(
            "SELECT MAX(id) FROM resources WHERE owner_key = ? AND title = ?",
            Long.class,
            USER_KEY,
            title
        );
        if (id == null) {
            throw new IllegalStateException("FAILED_TO_CREATE_RESOURCE");
        }
        return id;
    }

    private long createNote(String title) {
        jdbcTemplate.update(
            """
                INSERT INTO notes (title, content_md, owner_key, visibility, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
            title,
            "# " + title,
            USER_KEY,
            "PRIVATE",
            "DRAFT"
        );
        Long id = jdbcTemplate.queryForObject(
            "SELECT MAX(id) FROM notes WHERE owner_key = ? AND title = ?",
            Long.class,
            USER_KEY,
            title
        );
        if (id == null) {
            throw new IllegalStateException("FAILED_TO_CREATE_NOTE");
        }
        return id;
    }

    private long createPublishedPublicNote(String title) {
        jdbcTemplate.update(
            """
                INSERT INTO notes (title, content_md, owner_key, visibility, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
            title,
            "# " + title,
            USER_KEY,
            "PUBLIC",
            "PUBLISHED"
        );
        Long id = jdbcTemplate.queryForObject(
            "SELECT MAX(id) FROM notes WHERE owner_key = ? AND title = ?",
            Long.class,
            USER_KEY,
            title
        );
        if (id == null) {
            throw new IllegalStateException("FAILED_TO_CREATE_PUBLIC_NOTE");
        }
        return id;
    }

}
