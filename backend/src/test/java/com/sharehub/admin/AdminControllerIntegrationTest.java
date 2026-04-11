package com.sharehub.admin;

import com.sharehub.auth.RequestAccessService;
import com.sharehub.config.AdminTokenFilter;
import com.sharehub.interaction.InteractionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "sharehub.admin.dev-token-enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AdminControllerIntegrationTest {

    private static final String ADMIN_LOGIN = "playwright-admin";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private InteractionRepository interactionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanup() throws Exception {
        Files.deleteIfExists(Path.of("data/interaction.json"));
        jdbcTemplate.update("DELETE FROM admin_audit_logs");
        jdbcTemplate.update("DELETE FROM admin_accounts");
        jdbcTemplate.update("DELETE FROM reports");
        jdbcTemplate.update("DELETE FROM comments");
        jdbcTemplate.update("DELETE FROM favorites");
        jdbcTemplate.update("DELETE FROM likes");
        jdbcTemplate.update("DELETE FROM resources");
        jdbcTemplate.update("DELETE FROM users");
        seedAdminAccount();
    }

    @Test
    void reportsSupportsPaginationAndFilters() throws Exception {
        long firstResourceId = insertResource("举报资源-1");
        long secondResourceId = insertResource("举报资源-2");
        interactionRepository.saveReport(firstResourceId, "spam", "alice");
        interactionRepository.saveReport(secondResourceId, "abuse", "bob");

        mvc.perform(adminGet("/api/admin/reports")
                .param("page", "1")
                .param("pageSize", "1")
                .param("status", "OPEN")
                .param("targetType", "RESOURCE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.total").value(2))
            .andExpect(jsonPath("$.data.items[0].targetType").value("RESOURCE"));
    }

    @Test
    void auditLogsFilterActionAndTarget() throws Exception {
        interactionRepository.appendAuditLog("BLOCK_RESOURCE", "RESOURCE", "42", "{}");
        interactionRepository.appendAuditLog("HIDE_COMMENT", "COMMENT", "7", "{}");

        mvc.perform(adminGet("/api/admin/audit-logs")
                .param("page", "1")
                .param("pageSize", "10")
                .param("action", "BLOCK_RESOURCE")
                .param("targetType", "RESOURCE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.items[0].action").value("BLOCK_RESOURCE"))
            .andExpect(jsonPath("$.data.items[0].targetType").value("RESOURCE"));
    }

    @Test
    void adminEndpointsRejectMissingToken() throws Exception {
        mvc.perform(get("/api/admin/reports"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value(AdminTokenFilter.ADMIN_TOKEN_REQUIRED));

        mvc.perform(get("/api/admin"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value(AdminTokenFilter.ADMIN_TOKEN_REQUIRED));

        mvc.perform(get("/api/admin/audit-logs"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value(AdminTokenFilter.ADMIN_TOKEN_REQUIRED));

        mvc.perform(get("/api/admin/users"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value(AdminTokenFilter.ADMIN_TOKEN_REQUIRED));
    }

    @Test
    void adminEndpointsRejectNonAdminUserWithoutDevToken() throws Exception {
        insertUser("plain-user");

        mvc.perform(get("/api/admin/reports").header(RequestAccessService.USER_KEY_HEADER, "plain-user"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value(AdminTokenFilter.ADMIN_TOKEN_REQUIRED));
    }

    @Test
    void adminRootPathUsesWhitelistAuthentication() throws Exception {
        mvc.perform(adminGet("/api/admin"))
            .andExpect(status().isNotFound());
    }

    @Test
    void usersSupportsPaginationAndStatusMutation() throws Exception {
        long firstUserId = insertUser("admin-list-alpha");
        long secondUserId = insertUser("admin-list-beta");

        mvc.perform(adminGet("/api/admin/users")
                .param("page", "1")
                .param("pageSize", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.total").value(2))
            .andExpect(jsonPath("$.data.items[0].id").value(secondUserId))
            .andExpect(jsonPath("$.data.items[0].login").value("admin-list-beta"))
            .andExpect(jsonPath("$.data.items[0].status").value("ACTIVE"));

        mvc.perform(adminPost("/api/admin/users/" + firstUserId + "/ban"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("BANNED"));

        mvc.perform(adminGet("/api/admin/users")
                .param("page", "1")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(2))
            .andExpect(jsonPath("$.data.items[1].id").value(firstUserId))
            .andExpect(jsonPath("$.data.items[1].status").value("BANNED"));
    }

    @Test
    void resolveReportShouldRemoveResourceAndWriteAuditLog() throws Exception {
        long resourceId = insertResource("被举报资源");
        interactionRepository.saveReport(resourceId, "spam", "alice");

        long reportId = jdbcTemplate.queryForObject("SELECT id FROM reports WHERE target_id = ?", Long.class, resourceId);

        mvc.perform(adminPost("/api/admin/reports/" + reportId + "/resolve")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(reportId))
            .andExpect(jsonPath("$.data.status").value("RESOLVED"));

        mvc.perform(adminGet("/api/admin/audit-logs")
                .param("action", "AUTO_REMOVE_RESOURCE")
                .param("targetType", "RESOURCE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].targetId").value(String.valueOf(resourceId)));

        String resourceStatus = jdbcTemplate.queryForObject(
            "SELECT status FROM resources WHERE id = ?",
            String.class,
            resourceId
        );
        String reportStatus = jdbcTemplate.queryForObject(
            "SELECT status FROM reports WHERE id = ?",
            String.class,
            reportId
        );

        org.junit.jupiter.api.Assertions.assertEquals("REMOVED", resourceStatus);
        org.junit.jupiter.api.Assertions.assertEquals("RESOLVED", reportStatus);
    }

    @Test
    void moderationEndpointsShouldUpdateDomainState() throws Exception {
        long resourceId = insertResource("待封禁资源");
        long userId = insertUser("blocked-user");
        long commentId = insertComment(resourceId, "需要隐藏");

        mvc.perform(adminPost("/api/admin/resources/" + resourceId + "/block"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("REMOVED"));

        mvc.perform(adminPost("/api/admin/resources/" + resourceId + "/restore"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        mvc.perform(adminPost("/api/admin/users/" + userId + "/ban"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("BANNED"));

        mvc.perform(adminPost("/api/admin/users/" + userId + "/unban"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mvc.perform(adminGet("/api/admin/audit-logs")
                .param("targetType", "USER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(2))
            .andExpect(jsonPath("$.data.items[0].action").value("UNBAN_USER"))
            .andExpect(jsonPath("$.data.items[0].targetId").value(String.valueOf(userId)))
            .andExpect(jsonPath("$.data.items[1].action").value("BAN_USER"))
            .andExpect(jsonPath("$.data.items[1].targetId").value(String.valueOf(userId)));

        mvc.perform(adminPost("/api/admin/comments/" + commentId + "/hide"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("HIDDEN"));

        mvc.perform(adminPost("/api/admin/comments/" + commentId + "/restore"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("VISIBLE"));

        mvc.perform(adminGet("/api/admin/audit-logs")
                .param("targetType", "COMMENT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(2));

        org.junit.jupiter.api.Assertions.assertEquals(
            "PUBLISHED",
            jdbcTemplate.queryForObject("SELECT status FROM resources WHERE id = ?", String.class, resourceId)
        );
        org.junit.jupiter.api.Assertions.assertEquals(
            "ACTIVE",
            jdbcTemplate.queryForObject("SELECT status FROM users WHERE id = ?", String.class, userId)
        );
        org.junit.jupiter.api.Assertions.assertEquals(
            "VISIBLE",
            jdbcTemplate.queryForObject("SELECT status FROM comments WHERE id = ?", String.class, commentId)
        );
    }

    private MockHttpServletRequestBuilder adminGet(String uri) {
        return get(uri).header(RequestAccessService.USER_KEY_HEADER, ADMIN_LOGIN);
    }

    private MockHttpServletRequestBuilder adminPost(String uri) {
        return post(uri).header(RequestAccessService.USER_KEY_HEADER, ADMIN_LOGIN);
    }

    private long insertResource(String title) {
        jdbcTemplate.update(
            """
                INSERT INTO resources (title, type, summary, owner_key, visibility, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
            title,
            "PDF",
            "summary",
            "local-dev-user",
            "PUBLIC",
            "PUBLISHED",
            Timestamp.from(Instant.now()),
            Timestamp.from(Instant.now())
        );
        return jdbcTemplate.queryForObject("SELECT id FROM resources ORDER BY id DESC LIMIT 1", Long.class);
    }

    private long insertUser(String login) {
        jdbcTemplate.update(
            """
                INSERT INTO users (login, name, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?)
                """,
            login,
            "Tester",
            "ACTIVE",
            Timestamp.from(Instant.now()),
            Timestamp.from(Instant.now())
        );
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE login = ?", Long.class, login);
    }

    private long insertComment(long resourceId, String content) {
        jdbcTemplate.update(
            """
                INSERT INTO comments (resource_id, author_key, content, status, created_at)
                VALUES (?, ?, ?, ?, ?)
                """,
            resourceId,
            "local-dev-user",
            content,
            "VISIBLE",
            Timestamp.from(Instant.now())
        );
        return jdbcTemplate.queryForObject("SELECT id FROM comments ORDER BY id DESC LIMIT 1", Long.class);
    }

    private void seedAdminAccount() {
        jdbcTemplate.update(
            """
                INSERT INTO admin_accounts (
                    user_login, status, granted_by, granted_at, remark, created_at, updated_at
                ) VALUES (?, 'ACTIVE', ?, CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
            ADMIN_LOGIN,
            "admin-test",
            "admin integration test"
        );
    }
}
