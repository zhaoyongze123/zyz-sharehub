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
        jdbcTemplate.update("DELETE FROM notes");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void createListDetailUpdateDelete() throws Exception {
        NoteDto payload = new NoteDto(null, "Test", "# content", "PUBLIC", "DRAFT");
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
            .andExpect(jsonPath("$.data.title").value("Test"));

        payload = new NoteDto(id, "Updated", "# new", "PUBLIC", "PUBLISHED");
        mvc.perform(put("/api/notes/" + id)
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

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
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NOT_LOGGED_IN"));

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
        NoteDto payload = new NoteDto(null, "Owner Note", "# content", "PUBLIC", "DRAFT");
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
}
