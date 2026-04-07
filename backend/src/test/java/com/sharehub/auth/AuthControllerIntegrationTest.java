package com.sharehub.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    private static final String USER_KEY = "local-dev-user";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnCurrentUserProfileWhenUserHeaderProvided() throws Exception {
        mockMvc.perform(get("/api/auth/me").header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.message").value("OK"))
            .andExpect(jsonPath("$.data.login").value(USER_KEY))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void shouldPersistAvatarIntoUserProfile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "avatar.png",
            MediaType.IMAGE_PNG_VALUE,
            "png".getBytes()
        );

        mockMvc.perform(multipart("/api/auth/avatar")
                .file(file)
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.data.category").value("AVATAR"));

        mockMvc.perform(get("/api/auth/me").header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.avatarUrl").exists());
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
}
