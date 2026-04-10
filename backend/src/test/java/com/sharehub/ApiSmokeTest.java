package com.sharehub;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldGetGithubLoginUrl() throws Exception {
        mockMvc.perform(get("/api/auth/github/login"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.loginUrl").value("/oauth2/authorization/github"));
    }

    @Test
    void shouldCreateResource() throws Exception {
        String body = """
            {
              "title": "Spring Boot 资料",
              "type": "PDF",
              "summary": "测试",
              "visibility": "PUBLIC"
            }
            """;

        mockMvc.perform(post("/api/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.title").value("Spring Boot 资料"));
    }

    @Test
    void shouldPageResources() throws Exception {
        mockMvc.perform(get("/api/resources?page=0&pageSize=12"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items").isArray())
            .andExpect(jsonPath("$.data.page").value(0))
            .andExpect(jsonPath("$.data.pageSize").value(12));
    }
}
