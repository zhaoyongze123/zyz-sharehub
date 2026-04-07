package com.sharehub.me;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM favorites");
        jdbcTemplate.update("DELETE FROM roadmap_progress");
        jdbcTemplate.update("DELETE FROM roadmap_nodes");
        jdbcTemplate.update("DELETE FROM roadmaps");
        jdbcTemplate.update("DELETE FROM notes");
        jdbcTemplate.update("DELETE FROM resumes");
        jdbcTemplate.update("DELETE FROM resources");
        jdbcTemplate.update("DELETE FROM files");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void shouldAggregatePersonalCenterSummary() throws Exception {
        mockMvc.perform(post("/api/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"我的资料","type":"PDF","visibility":"PUBLIC"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/roadmaps")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"我的路线","description":"desc","visibility":"PUBLIC"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"我的笔记","contentMd":"# note","visibility":"PUBLIC","status":"DRAFT"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/resumes/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("templateKey", "me"))))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/resources/1/favorite"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.profile.login").value("local-dev-user"))
            .andExpect(jsonPath("$.data.myResourceCount").value(1))
            .andExpect(jsonPath("$.data.myFavoriteCount").value(1))
            .andExpect(jsonPath("$.data.myRoadmapCount").value(1))
            .andExpect(jsonPath("$.data.myNoteCount").value(1))
            .andExpect(jsonPath("$.data.myResumeCount").value(1));
    }
}
