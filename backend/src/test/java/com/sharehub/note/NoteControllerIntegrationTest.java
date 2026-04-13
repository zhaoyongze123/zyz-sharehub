package com.sharehub.note;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sharehub.auth.RequestAccessService;
import com.sharehub.auth.UserProfileDto;
import com.sharehub.auth.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class NoteControllerIntegrationTest {

    private static final String USER_KEY = "note-user";
    private static final String OTHER_USER_KEY = "note-other-user";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @org.junit.jupiter.api.BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM note_view_history");
        jdbcTemplate.update("DELETE FROM note_tags");
        jdbcTemplate.update("DELETE FROM tags");
        jdbcTemplate.update("DELETE FROM notes");
        jdbcTemplate.update("DELETE FROM admin_whitelist");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void createListDetailUpdateDelete() throws Exception {
        NoteDto payload = new NoteDto(null, "Test", "# content", "PUBLIC", "DRAFT", "AI 应用与 Agent", java.util.List.of("spring", "agent"), null, null, null, null, null, false, false);
        String body = mapper.writeValueAsString(payload);

        String response = mvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();

        Map<?, ?> created = mapper.readValue(response, Map.class);
        Map<?, ?> data = (Map<?, ?>) created.get("data");
        Long id = Long.valueOf(data.get("id").toString());

        mvc.perform(get("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .param("page", "1")
                .param("pageSize", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items").isArray())
            .andExpect(jsonPath("$.data.total").value(1));

        mvc.perform(get("/api/notes/" + id)
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("Test"))
            .andExpect(jsonPath("$.data.tags", containsInAnyOrder("spring", "agent")));

        payload = new NoteDto(id, "Updated", "# new", "PUBLIC", "PUBLISHED", "提示词工程", java.util.List.of("prompt", "workflow"), null, null, null, null, null, false, false);
        mvc.perform(put("/api/notes/" + id)
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
            .andExpect(jsonPath("$.data.tags", containsInAnyOrder("prompt", "workflow")));

        mvc.perform(delete("/api/notes/" + id)
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("DELETED"));
    }

    @Test
    void shouldRejectAnonymousAccess() throws Exception {
        mvc.perform(get("/api/notes"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NOT_LOGGED_IN"));

        mvc.perform(post("/api/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Test","contentMd":"# content","visibility":"PUBLIC","status":"DRAFT"}
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NOT_LOGGED_IN"));

        mvc.perform(get("/api/notes/1"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOTE_NOT_FOUND"));

        mvc.perform(put("/api/notes/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Updated","contentMd":"# new","visibility":"PUBLIC","status":"PUBLISHED"}
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NOT_LOGGED_IN"));

        mvc.perform(delete("/api/notes/1"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NOT_LOGGED_IN"));
    }

    @Test
    void shouldRejectBannedUserAccess() throws Exception {
        UserProfileDto bannedUser = userProfileRepository.upsert("note-banned-user", "note-banned-user", null);
        userProfileRepository.updateStatus(bannedUser.id(), "BANNED");

        mvc.perform(get("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, bannedUser.login()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("USER_BANNED"));

        mvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, bannedUser.login())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Test","contentMd":"# content","visibility":"PUBLIC","status":"DRAFT"}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("USER_BANNED"));

        mvc.perform(get("/api/notes/1")
                .header(RequestAccessService.USER_KEY_HEADER, bannedUser.login()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("USER_BANNED"));

        mvc.perform(put("/api/notes/1")
                .header(RequestAccessService.USER_KEY_HEADER, bannedUser.login())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Updated","contentMd":"# new","visibility":"PUBLIC","status":"PUBLISHED"}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("USER_BANNED"));

        mvc.perform(delete("/api/notes/1")
                .header(RequestAccessService.USER_KEY_HEADER, bannedUser.login()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("USER_BANNED"));
    }

    @Test
    void shouldRestrictNotesToOwner() throws Exception {
        NoteDto payload = new NoteDto(null, "Owner Note", "# content", "PUBLIC", "DRAFT", "学术与论文", java.util.List.of(), null, null, null, null, null, false, false);
        String response = mvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Map<?, ?> created = mapper.readValue(response, Map.class);
        Map<?, ?> data = (Map<?, ?>) created.get("data");
        Long id = Long.valueOf(data.get("id").toString());

        mvc.perform(get("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, OTHER_USER_KEY)
                .param("page", "1")
                .param("pageSize", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(0));

        mvc.perform(get("/api/notes/" + id)
                .header(RequestAccessService.USER_KEY_HEADER, OTHER_USER_KEY))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOTE_NOT_FOUND"));

        mvc.perform(put("/api/notes/" + id)
                .header(RequestAccessService.USER_KEY_HEADER, OTHER_USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Updated","contentMd":"# new","visibility":"PUBLIC","status":"PUBLISHED"}
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOTE_NOT_FOUND"));

        mvc.perform(delete("/api/notes/" + id)
                .header(RequestAccessService.USER_KEY_HEADER, OTHER_USER_KEY))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOTE_NOT_FOUND"));
    }

    @Test
    void shouldAllowAdminToDeleteOtherUsersNote() throws Exception {
        String response = mvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Owner Note","contentMd":"# content","visibility":"PUBLIC","status":"PUBLISHED"}
                    """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long id = Long.valueOf(((Map<?, ?>) ((Map<?, ?>) mapper.readValue(response, Map.class)).get("data")).get("id").toString());

        jdbcTemplate.update(
            "INSERT INTO admin_whitelist (github_login, role, created_by, created_at, updated_at) VALUES (?, 'ADMIN', 'test', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            OTHER_USER_KEY
        );

        mvc.perform(delete("/api/notes/" + id)
                .header(RequestAccessService.USER_KEY_HEADER, OTHER_USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("DELETED"));

        mvc.perform(get("/api/notes/" + id)
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOTE_NOT_FOUND"));
    }

    @Test
    void shouldListCommunityPublishedNotesWithCategory() throws Exception {
        long firstId = createPublishedNote(USER_KEY, "社区公开-Agent", "# 社区公开-Agent\n\n验证公开列表");
        jdbcTemplate.update(
            """
                UPDATE notes
                SET category = 'AI 应用与 Agent'
                WHERE owner_key = ? AND title = ?
                """,
            USER_KEY,
            "社区公开-Agent"
        );
        jdbcTemplate.update(
            """
                INSERT INTO tags (name, slug, type, status, created_at, updated_at)
                VALUES ('agent', 'agent', 'CONTENT', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update(
            """
                INSERT INTO note_tags (note_id, tag_id, created_at)
                SELECT ?, id, CURRENT_TIMESTAMP FROM tags WHERE slug = 'agent'
                """,
            firstId
        );
        long secondId = createPublishedNote(OTHER_USER_KEY, "社区公开-提示词", "# 社区公开-提示词\n\n验证跨用户公开列表");
        jdbcTemplate.update(
            """
                UPDATE notes
                SET category = '提示词工程'
                WHERE owner_key = ? AND title = ?
                """,
            OTHER_USER_KEY,
            "社区公开-提示词"
        );
        jdbcTemplate.update(
            """
                INSERT INTO tags (name, slug, type, status, created_at, updated_at)
                VALUES ('prompt', 'prompt', 'CONTENT', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update(
            """
                INSERT INTO note_tags (note_id, tag_id, created_at)
                SELECT ?, id, CURRENT_TIMESTAMP FROM tags WHERE slug = 'prompt'
                """,
            secondId
        );
        jdbcTemplate.update(
            """
                INSERT INTO notes (title, content_md, owner_key, visibility, status, category, created_at, updated_at)
                VALUES ('社区草稿', '# 社区草稿', ?, 'PRIVATE', 'DRAFT', '学术与论文', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
            OTHER_USER_KEY
        );

        mvc.perform(get("/api/notes/community")
                .param("page", "1")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(2))
            .andExpect(jsonPath("$.data.items[0].title").value("社区公开-提示词"))
            .andExpect(jsonPath("$.data.items[0].category").value("提示词工程"))
            .andExpect(jsonPath("$.data.items[0].tags", containsInAnyOrder("prompt")))
            .andExpect(jsonPath("$.data.items[1].title").value("社区公开-Agent"))
            .andExpect(jsonPath("$.data.items[1].category").value("AI 应用与 Agent"))
            .andExpect(jsonPath("$.data.items[1].tags", containsInAnyOrder("agent")));
    }

    @Test
    void shouldPersistAndReturnNoteTagsFromStandardTables() throws Exception {
        String response = mvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Tag Note","contentMd":"# content","visibility":"PUBLIC","status":"PUBLISHED","tags":["java","spring","java"]}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.tags.length()").value(2))
            .andExpect(jsonPath("$.data.tags[0]").value("java"))
            .andExpect(jsonPath("$.data.tags[1]").value("spring"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long id = Long.valueOf(((Map<?, ?>) ((Map<?, ?>) mapper.readValue(response, Map.class)).get("data")).get("id").toString());

        Integer noteTagCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM note_tags WHERE note_id = ?",
            Integer.class,
            id
        );
        org.assertj.core.api.Assertions.assertThat(noteTagCount).isEqualTo(2);

        mvc.perform(get("/api/notes/community")
                .param("page", "1")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].id").value(id))
            .andExpect(jsonPath("$.data.items[0].tags", containsInAnyOrder("java", "spring")));
    }

    @Test
    void shouldAllowAnonymousAccessToPublishedPublicNoteDetail() throws Exception {
        String response = mvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Public Published Note","contentMd":"# content","visibility":"PUBLIC","status":"PUBLISHED"}
                    """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Map<?, ?> created = mapper.readValue(response, Map.class);
        Map<?, ?> data = (Map<?, ?>) created.get("data");
        Long id = Long.valueOf(data.get("id").toString());

        mvc.perform(get("/api/notes/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(id))
            .andExpect(jsonPath("$.data.title").value("Public Published Note"))
            .andExpect(jsonPath("$.data.visibility").value("PUBLIC"))
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    void shouldAllowOtherUsersToReadPublishedPublicNoteDetailOnly() throws Exception {
        String publicResponse = mvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Public Published Note","contentMd":"# content","visibility":"PUBLIC","status":"PUBLISHED"}
                    """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        Long publicId = Long.valueOf(((Map<?, ?>) ((Map<?, ?>) mapper.readValue(publicResponse, Map.class)).get("data")).get("id").toString());

        String privateResponse = mvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Private Draft Note","contentMd":"# content","visibility":"PRIVATE","status":"DRAFT"}
                    """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        Long privateId = Long.valueOf(((Map<?, ?>) ((Map<?, ?>) mapper.readValue(privateResponse, Map.class)).get("data")).get("id").toString());

        mvc.perform(get("/api/notes/" + publicId)
                .header(RequestAccessService.USER_KEY_HEADER, OTHER_USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("Public Published Note"));

        mvc.perform(get("/api/notes/" + privateId)
                .header(RequestAccessService.USER_KEY_HEADER, OTHER_USER_KEY))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOTE_NOT_FOUND"));
    }

    @Test
    void shouldFilterNotesByStatus() throws Exception {
        mvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Draft Note","contentMd":"# draft","visibility":"PUBLIC","status":"DRAFT"}
                    """))
            .andExpect(status().isOk());

        mvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Published Note","contentMd":"# published","visibility":"PUBLIC","status":"PUBLISHED"}
                    """))
            .andExpect(status().isOk());

        mvc.perform(get("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .param("status", "PUBLISHED")
                .param("page", "1")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].title").value("Published Note"))
            .andExpect(jsonPath("$.data.items[0].status").value("PUBLISHED"));
    }

    @Test
    void shouldTrimNoteStatusFilterBeforeApplying() throws Exception {
        mvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Draft Note","contentMd":"# draft","visibility":"PUBLIC","status":"DRAFT"}
                    """))
            .andExpect(status().isOk());

        mvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Published Note","contentMd":"# published","visibility":"PUBLIC","status":"PUBLISHED"}
                    """))
            .andExpect(status().isOk());

        mvc.perform(get("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .param("status", " PUBLISHED ")
                .param("page", "1")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].title").value("Published Note"))
            .andExpect(jsonPath("$.data.items[0].status").value("PUBLISHED"));
    }

    @Test
    void shouldTreatBlankNoteStatusAsUnfiltered() throws Exception {
        mvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Draft Note","contentMd":"# draft","visibility":"PUBLIC","status":"DRAFT"}
                    """))
            .andExpect(status().isOk());

        mvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Published Note","contentMd":"# published","visibility":"PUBLIC","status":"PUBLISHED"}
                    """))
            .andExpect(status().isOk());

        mvc.perform(get("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .param("status", "   ")
                .param("page", "1")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(2))
            .andExpect(jsonPath("$.data.items[0].title").value("Published Note"))
            .andExpect(jsonPath("$.data.items[1].title").value("Draft Note"));
    }

    @Test
    void shouldFallbackInvalidPaginationParamsToOne() throws Exception {
        mvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"First Note","contentMd":"# first","visibility":"PUBLIC","status":"DRAFT"}
                    """))
            .andExpect(status().isOk());

        mvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Second Note","contentMd":"# second","visibility":"PUBLIC","status":"PUBLISHED"}
                    """))
            .andExpect(status().isOk());

        mvc.perform(get("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .param("page", "0")
                .param("pageSize", "0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.page").value(1))
            .andExpect(jsonPath("$.data.pageSize").value(1))
            .andExpect(jsonPath("$.data.total").value(2))
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.items[0].title").value("Second Note"));
    }

    @Test
    void shouldDefaultStatusToDraftWhenMissing() throws Exception {
        mvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"No Status Note","contentMd":"# content","visibility":"PUBLIC"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("DRAFT"));

        mvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Null Status Note","contentMd":"# content","visibility":"PUBLIC","status":null}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("DRAFT"));

        mvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Blank Status Note","contentMd":"# content","visibility":"PUBLIC","status":""}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("DRAFT"));

        mvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Whitespace Status Note","contentMd":"# content","visibility":"PUBLIC","status":"   "}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void shouldTrimStatusOnCreateAndUpdate() throws Exception {
        String response = mvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Trimmed Status Note","contentMd":"# content","visibility":"PUBLIC","status":" PUBLISHED "}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        Map<?, ?> created = mapper.readValue(response, Map.class);
        Map<?, ?> data = (Map<?, ?>) created.get("data");
        Long id = Long.valueOf(data.get("id").toString());

        mvc.perform(put("/api/notes/" + id)
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Trimmed Status Note","contentMd":"# updated","visibility":"PUBLIC","status":" DRAFT "}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("DRAFT"));

        mvc.perform(get("/api/notes/" + id)
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void shouldTreatBlankVisibilityAsNullOnCreateAndUpdate() throws Exception {
        String response = mvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Blank Visibility Note","contentMd":"# content","visibility":"   ","status":"DRAFT"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.visibility").value(nullValue()))
            .andReturn()
            .getResponse()
            .getContentAsString();

        Map<?, ?> created = mapper.readValue(response, Map.class);
        Map<?, ?> data = (Map<?, ?>) created.get("data");
        Long id = Long.valueOf(data.get("id").toString());

        mvc.perform(put("/api/notes/" + id)
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Blank Visibility Note","contentMd":"# content","visibility":"PUBLIC","status":"DRAFT"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.visibility").value("PUBLIC"));

        mvc.perform(put("/api/notes/" + id)
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Blank Visibility Note","contentMd":"# content","visibility":"    ","status":"DRAFT"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.visibility").value(nullValue()));

        mvc.perform(get("/api/notes/" + id)
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.visibility").value(nullValue()));
    }

    @Test
    void shouldTreatNullVisibilityAsNullOnUpdate() throws Exception {
        String response = mvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Null Visibility Note","contentMd":"# content","visibility":"PUBLIC","status":"DRAFT"}
                    """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Map<?, ?> created = mapper.readValue(response, Map.class);
        Map<?, ?> data = (Map<?, ?>) created.get("data");
        Long id = Long.valueOf(data.get("id").toString());

        mvc.perform(put("/api/notes/" + id)
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Null Visibility Note","contentMd":"# content","visibility":null,"status":"DRAFT"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.visibility").value(nullValue()));

        mvc.perform(get("/api/notes/" + id)
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.visibility").value(nullValue()));
    }

    @Test
    void shouldKeepExistingStatusWhenUpdatingWithBlankStatus() throws Exception {
        String response = mvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Original Note","contentMd":"# content","visibility":"PUBLIC","status":"PUBLISHED"}
                    """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Map<?, ?> created = mapper.readValue(response, Map.class);
        Map<?, ?> data = (Map<?, ?>) created.get("data");
        Long id = Long.valueOf(data.get("id").toString());

        mvc.perform(put("/api/notes/" + id)
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Updated Note","contentMd":"# updated","visibility":"PUBLIC","status":"   "}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        mvc.perform(get("/api/notes/" + id)
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    void shouldKeepExistingStatusWhenUpdatingWithNullStatus() throws Exception {
        String response = mvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Original Note","contentMd":"# content","visibility":"PUBLIC","status":"PUBLISHED"}
                    """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Map<?, ?> created = mapper.readValue(response, Map.class);
        Map<?, ?> data = (Map<?, ?>) created.get("data");
        Long id = Long.valueOf(data.get("id").toString());

        mvc.perform(put("/api/notes/" + id)
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Updated Note","contentMd":"# updated","visibility":"PUBLIC","status":null}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        mvc.perform(get("/api/notes/" + id)
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    void shouldReturnRelatedAccessibleNotes() throws Exception {
        long sourceId = createPublishedNote(USER_KEY, "Spring Agent 实战", "# Spring Agent 实战\n\n共享 MCP 调度与审计链路");
        createPublishedNote(USER_KEY, "Spring Agent 清单", "# Spring Agent 清单\n\n共享 MCP 编排");
        createPublishedNote(USER_KEY, "Redis 缓存", "# Redis 缓存\n\n与当前主题无关");
        createPublishedNote(OTHER_USER_KEY, "Spring Agent 对照", "# Spring Agent 对照\n\n共享 MCP 调度");

        mvc.perform(get("/api/notes/" + sourceId + "/related")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(2)))
            .andExpect(jsonPath("$.data[0].id").isNumber())
            .andExpect(jsonPath("$.data[0].title").exists())
            .andExpect(jsonPath("$.data[0].summary").exists());
    }

    private long createPublishedNote(String ownerKey, String title, String contentMd) {
        jdbcTemplate.update(
            """
                INSERT INTO notes (title, content_md, owner_key, visibility, status, created_at, updated_at)
                VALUES (?, ?, ?, 'PUBLIC', 'PUBLISHED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
            title,
            contentMd,
            ownerKey
        );
        Long id = jdbcTemplate.queryForObject(
            "SELECT MAX(id) FROM notes WHERE owner_key = ? AND title = ?",
            Long.class,
            ownerKey,
            title
        );
        if (id == null) {
            throw new IllegalStateException("FAILED_TO_CREATE_PUBLISHED_NOTE");
        }
        return id;
    }
}
