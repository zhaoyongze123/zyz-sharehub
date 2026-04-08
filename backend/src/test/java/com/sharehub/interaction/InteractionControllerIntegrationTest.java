package com.sharehub.interaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sharehub.auth.RequestAccessService;
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

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM admin_audit_logs");
        jdbcTemplate.update("DELETE FROM reports");
        jdbcTemplate.update("DELETE FROM comments");
        jdbcTemplate.update("DELETE FROM favorites");
        jdbcTemplate.update("DELETE FROM likes");
    }

    @Test
    void commentReplyFavoriteLikeReportPersisted() throws Exception {
        var commentPayload = mapper.writeValueAsString(Map.of("content", "hello"));
        mvc.perform(post("/api/resources/1/comments")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(commentPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.resourceId").value(1));

        var replyPayload = mapper.writeValueAsString(Map.of("content", "reply"));
        mvc.perform(post("/api/comments/1/reply")
                        .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(replyPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parentId").value(1))
                .andExpect(jsonPath("$.data.resourceId").value(1));

        mvc.perform(post("/api/resources/1/favorite")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.favorites").value(1));

        mvc.perform(post("/api/resources/1/like")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.likes").value(1));

        var reportPayload = mapper.writeValueAsString(Map.of("resourceId", "1", "reason", "spam"));
        mvc.perform(post("/api/reports")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reportPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("OPEN"))
            .andExpect(jsonPath("$.data.targetId").value(1));

        mvc.perform(get("/api/resources/1/comments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].children", hasSize(1)));

        mvc.perform(get("/api/resources/1/interactions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.resourceId").value(1))
            .andExpect(jsonPath("$.data.comments").value(2))
            .andExpect(jsonPath("$.data.favorites").value(1))
            .andExpect(jsonPath("$.data.likes").value(1))
            .andExpect(jsonPath("$.data.reports").value(1));

        mvc.perform(post("/api/resources/1/favorite")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.favorites").value(1));

        mvc.perform(delete("/api/resources/1/favorite")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.favorites").value(0));

        mvc.perform(delete("/api/resources/1/favorite")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.favorites").value(0));

        mvc.perform(post("/api/resources/1/like")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.likes").value(1));

        mvc.perform(delete("/api/resources/1/like")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.likes").value(0));

        mvc.perform(delete("/api/resources/1/like")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.likes").value(0));

        mvc.perform(get("/api/resources/1/interactions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.favorites").value(0))
            .andExpect(jsonPath("$.data.likes").value(0));

        mvc.perform(adminPost("/api/admin/comments/2/hide"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("HIDDEN"));

        mvc.perform(get("/api/resources/1/comments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].children", hasSize(0)));

        mvc.perform(get("/api/resources/1/interactions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.comments").value(1));

        mvc.perform(adminPost("/api/admin/comments/2/restore"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("VISIBLE"));

        mvc.perform(get("/api/resources/1/comments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].children", hasSize(1)));

        mvc.perform(get("/api/resources/1/interactions"))
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

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder adminPost(String uri) {
        return post(uri).header(AdminTokenFilter.HEADER, AdminTokenFilter.DEFAULT_ADMIN_TOKEN);
    }
}
