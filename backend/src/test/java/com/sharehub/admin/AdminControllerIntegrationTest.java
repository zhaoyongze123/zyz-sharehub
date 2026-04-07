package com.sharehub.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @BeforeEach
    void cleanup() throws Exception {
        Files.deleteIfExists(Path.of("data/interaction.json"));
    }

    @Test
    void listAndResolveReports() throws Exception {
        var payload = mapper.writeValueAsString(Map.of("resourceId", "42", "reason", "spam"));
        mvc.perform(post("/api/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk());

        mvc.perform(get("/api/admin/reports"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].status").value("OPEN"));

        mvc.perform(post("/api/admin/reports/1/resolve"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("RESOLVED"));
    }
}
