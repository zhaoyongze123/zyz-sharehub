package com.sharehub.resume;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
            .andExpect(jsonPath("$.data.fileUrl").value("/api/resumes/" + id + "/download"));

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

        mockMvc.perform(get("/api/resumes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].id").value(id));

        mockMvc.perform(delete("/api/resumes/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("DELETED"));

        mockMvc.perform(get("/api/resumes/" + id))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/resumes/" + id + "/download"))
            .andExpect(status().isNotFound());
    }
}
