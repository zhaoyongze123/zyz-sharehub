package com.sharehub.auth;

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
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sharehub.auth.UserProfileDto;
import com.sharehub.auth.UserProfileRepository;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "sharehub.auth.dev-user-header-enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    private static final String USER_KEY = "local-dev-user";
    private static final String BANNED_USER_KEY = "banned-user";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void cleanupAdminAccounts() {
        jdbcTemplate.update("DELETE FROM admin_accounts");
    }

    @Test
    void shouldReturnCurrentUserProfileWhenUserHeaderProvided() throws Exception {
        mockMvc.perform(get("/api/auth/me").header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.message").value("OK"))
            .andExpect(jsonPath("$.data.login").value(USER_KEY))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
            .andExpect(jsonPath("$.data.isAdmin").value(false));
    }

    @Test
    void shouldNotExposeAdminFlagForWhitelistedAdminViaDevHeader() throws Exception {
        String adminLogin = "auth-admin";
        grantAdmin(adminLogin);

        mockMvc.perform(get("/api/auth/me").header(RequestAccessService.USER_KEY_HEADER, adminLogin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.login").value(adminLogin))
            .andExpect(jsonPath("$.data.isAdmin").value(false));
    }

    @Test
    void shouldExposeAdminFlagForWhitelistedOauthAdmin() throws Exception {
        String adminLogin = "oauth-auth-admin";
        grantAdmin(adminLogin);

        mockMvc.perform(
                get("/api/auth/me")
                    .with(oauth2Login().attributes(attributes -> {
                        attributes.put("login", adminLogin);
                        attributes.put("name", "OAuth Auth Admin");
                    }))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.login").value(adminLogin))
            .andExpect(jsonPath("$.data.name").value("OAuth Auth Admin"))
            .andExpect(jsonPath("$.data.isAdmin").value(true));
    }

    @Test
    void shouldNotExposeAdminFlagForPlainOauthUser() throws Exception {
        String oauthLogin = "plain-oauth-user";

        mockMvc.perform(
                get("/api/auth/me")
                    .with(oauth2Login().attributes(attributes -> {
                        attributes.put("login", oauthLogin);
                        attributes.put("name", "Plain OAuth User");
                    }))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.login").value(oauthLogin))
            .andExpect(jsonPath("$.data.name").value("Plain OAuth User"))
            .andExpect(jsonPath("$.data.isAdmin").value(false));
    }

    @Test
    void shouldNotExposeAdminFlagForRevokedAdminAccount() throws Exception {
        String adminLogin = "revoked-auth-admin";
        revokeAdmin(adminLogin);

        mockMvc.perform(get("/api/auth/me").header(RequestAccessService.USER_KEY_HEADER, adminLogin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.login").value(adminLogin))
            .andExpect(jsonPath("$.data.isAdmin").value(false));
    }

    @Test
    void shouldPersistAvatarIntoUserProfile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "avatar.png",
            MediaType.IMAGE_PNG_VALUE,
            "png".getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/auth/avatar")
                .file(file)
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.data.category").value("AVATAR"))
            .andExpect(jsonPath("$.data.owner").value(USER_KEY))
            .andExpect(jsonPath("$.data.filename").value("avatar.png"))
            .andExpect(jsonPath("$.data.downloadUrl").exists())
            .andReturn();

        JsonNode uploadJson = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        String downloadUrl = uploadJson.get("data").get("downloadUrl").asText();

        MvcResult downloadResult = mockMvc.perform(get(downloadUrl))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE))
            .andReturn();

        assertThat(downloadResult.getResponse().getContentAsByteArray()).isEqualTo("png".getBytes());

        mockMvc.perform(get("/api/auth/me").header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.avatarUrl").exists());
    }

    @Test
    void shouldFallbackAvatarContentTypeWhenUploadContentTypeMissing() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "avatar.bin",
            null,
            "avatar-binary".getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/auth/avatar")
                .file(file)
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.category").value("AVATAR"))
            .andExpect(jsonPath("$.data.filename").value("avatar.bin"))
            .andExpect(jsonPath("$.data.contentType").value(MediaType.APPLICATION_OCTET_STREAM_VALUE))
            .andExpect(jsonPath("$.data.downloadUrl").exists())
            .andReturn();

        JsonNode uploadJson = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        String downloadUrl = uploadJson.get("data").get("downloadUrl").asText();

        mockMvc.perform(get(downloadUrl))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"avatar.bin\""));
    }

    @Test
    void shouldFallbackAvatarContentTypeWhenUploadContentTypeBlank() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "avatar-blank.bin",
            " ",
            "avatar-binary".getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/auth/avatar")
                .file(file)
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.category").value("AVATAR"))
            .andExpect(jsonPath("$.data.filename").value("avatar-blank.bin"))
            .andExpect(jsonPath("$.data.contentType").value(MediaType.APPLICATION_OCTET_STREAM_VALUE))
            .andExpect(jsonPath("$.data.downloadUrl").exists())
            .andReturn();

        JsonNode uploadJson = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        String downloadUrl = uploadJson.get("data").get("downloadUrl").asText();

        mockMvc.perform(get(downloadUrl))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"avatar-blank.bin\""));
    }

    @Test
    void shouldFallbackAvatarContentTypeWhenUploadContentTypeInvalid() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "avatar-invalid.bin",
            "invalid/type;",
            "avatar-binary".getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/auth/avatar")
                .file(file)
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.category").value("AVATAR"))
            .andExpect(jsonPath("$.data.filename").value("avatar-invalid.bin"))
            .andExpect(jsonPath("$.data.contentType").value(MediaType.APPLICATION_OCTET_STREAM_VALUE))
            .andExpect(jsonPath("$.data.downloadUrl").exists())
            .andReturn();

        JsonNode uploadJson = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        String downloadUrl = uploadJson.get("data").get("downloadUrl").asText();

        mockMvc.perform(get(downloadUrl))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"avatar-invalid.bin\""));
    }

    @Test
    void shouldRejectAnonymousRequestsForCurrentUserEndpoints() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "avatar.png",
            MediaType.IMAGE_PNG_VALUE,
            "png".getBytes()
        );

        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NOT_LOGGED_IN"));

        mockMvc.perform(multipart("/api/auth/avatar").file(file))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NOT_LOGGED_IN"));
    }

    @Test
    void shouldRejectBannedUserWhenFetchingProfile() throws Exception {
        UserProfileDto bannedUser = userProfileRepository.upsert(BANNED_USER_KEY, BANNED_USER_KEY, null);
        userProfileRepository.updateStatus(bannedUser.id(), "BANNED");

        mockMvc.perform(get("/api/auth/me").header(RequestAccessService.USER_KEY_HEADER, bannedUser.login()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("USER_BANNED"))
            .andExpect(jsonPath("$.message").value("USER_BANNED"));
    }

    @Test
    void shouldRejectAvatarUploadForBannedUser() throws Exception {
        UserProfileDto bannedUser = userProfileRepository.upsert(BANNED_USER_KEY + "-avatar", BANNED_USER_KEY, null);
        userProfileRepository.updateStatus(bannedUser.id(), "BANNED");

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "avatar.png",
            MediaType.IMAGE_PNG_VALUE,
            "png".getBytes()
        );

        mockMvc.perform(multipart("/api/auth/avatar")
                .file(file)
                .header(RequestAccessService.USER_KEY_HEADER, bannedUser.login()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("USER_BANNED"))
            .andExpect(jsonPath("$.message").value("USER_BANNED"));
    }

    private void grantAdmin(String login) {
        jdbcTemplate.update(
            """
                INSERT INTO admin_accounts (
                    user_login, status, granted_by, granted_at, remark, created_at, updated_at
                ) VALUES (?, 'ACTIVE', ?, CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
            login,
            "auth-test",
            "auth integration test"
        );
    }

    private void revokeAdmin(String login) {
        jdbcTemplate.update(
            """
                INSERT INTO admin_accounts (
                    user_login, status, granted_by, granted_at, revoked_by, revoked_at, remark, created_at, updated_at
                ) VALUES (?, 'REVOKED', ?, CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
            login,
            "auth-test",
            "security-audit",
            "revoked auth integration test"
        );
    }
}
