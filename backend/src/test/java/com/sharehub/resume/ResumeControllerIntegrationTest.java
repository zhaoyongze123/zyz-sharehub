package com.sharehub.resume;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResumeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM resumes");
        jdbcTemplate.update("DELETE FROM files");
    }

    @Test
    void shouldGenerateDetailAndDownloadResume() throws Exception {
        String response = mockMvc.perform(post("/api/resumes/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("templateKey", "classic"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.templateKey").value("classic"))
            .andExpect(jsonPath("$.data.status").value("GENERATED"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        Map<?, ?> payload = objectMapper.readValue(response, Map.class);
        Map<?, ?> data = (Map<?, ?>) payload.get("data");
        Long id = Long.valueOf(String.valueOf(data.get("id")));

        mockMvc.perform(get("/api/resumes/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.fileUrl").value("/api/resumes/" + id + "/download"))
            .andExpect(jsonPath("$.data.fileName").value("resume-classic.pdf"))
            .andExpect(jsonPath("$.data.fileSize").isNumber())
            .andExpect(jsonPath("$.data.fileCreatedAt").exists())
            .andExpect(jsonPath("$.data.fileUpdatedAt").exists());

        mockMvc.perform(get("/api/resumes/" + id + "/download"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE));
    }

    @Test
    void shouldListAndDeleteResume() throws Exception {
        String response = mockMvc.perform(post("/api/resumes/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("templateKey", "classic"))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Map<?, ?> payload = objectMapper.readValue(response, Map.class);
        Map<?, ?> data = (Map<?, ?>) payload.get("data");
        Long id = Long.valueOf(String.valueOf(data.get("id")));

        mockMvc.perform(get("/api/resumes").param("status", "GENERATED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].id").value(id.intValue()))
            .andExpect(jsonPath("$.data.items[0].fileName").value("resume-classic.pdf"))
            .andExpect(jsonPath("$.data.items[0].fileSize").isNumber());

        mockMvc.perform(delete("/api/resumes/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("DELETED"));

        mockMvc.perform(get("/api/resumes/" + id))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/resumes/" + id + "/download"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnResumeWorkbenchSummary() throws Exception {
        mockMvc.perform(post("/api/resumes/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("templateKey", "classic"))))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/resumes/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("templateKey", "classic"))))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/resumes/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("templateKey", "modern"))))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/resumes/workbench"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(3))
            .andExpect(jsonPath("$.data.generatedCount").value(3))
            .andExpect(jsonPath("$.data.templateBreakdown[0].templateKey").value("classic"))
            .andExpect(jsonPath("$.data.templateBreakdown[0].count").value(2))
            .andExpect(jsonPath("$.data.templateBreakdown[1].templateKey").value("modern"))
            .andExpect(jsonPath("$.data.templateBreakdown[1].count").value(1))
            .andExpect(jsonPath("$.data.recentItems.length()").value(3));
    }
}
