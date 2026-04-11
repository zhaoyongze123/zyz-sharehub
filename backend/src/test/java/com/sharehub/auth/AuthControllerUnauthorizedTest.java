package com.sharehub.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
        "sharehub.auth.require-login=true",
        "sharehub.auth.dev-user-header-enabled=false"
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerUnauthorizedTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsUnauthorizedWhenAccessingProfileWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("NOT_LOGGED_IN"))
            .andExpect(jsonPath("$.message").value("NOT_LOGGED_IN"));
    }

    @Test
    void returnsUnauthorizedWhenDevUserHeaderDisabled() throws Exception {
        mockMvc.perform(get("/api/auth/me").header(RequestAccessService.USER_KEY_HEADER, "spoofed-user"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("NOT_LOGGED_IN"))
            .andExpect(jsonPath("$.message").value("NOT_LOGGED_IN"));
    }

    @Test
    void avatarUploadBlockedWhenNotLoggedIn() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "avatar.png",
            MediaType.IMAGE_PNG_VALUE,
            "png".getBytes()
        );

        mockMvc.perform(multipart("/api/auth/avatar").file(file))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("NOT_LOGGED_IN"))
            .andExpect(jsonPath("$.message").value("NOT_LOGGED_IN"));
    }
}
