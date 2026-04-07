package com.sharehub.interaction;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class InteractionControllerIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @BeforeEach
    void reset() throws Exception {
        Files.deleteIfExists(Path.of("data/interaction.json"));
    }

    @Test
    void commentReplyFavoriteLikeReportPersisted() throws Exception {
        var commentPayload = mapper.writeValueAsString(Map.of("content", "hello"));
        mvc.perform(post("/api/resources/1/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(commentPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.resourceId").value(1));

        var replyPayload = mapper.writeValueAsString(Map.of("content", "reply"));
        mvc.perform(post("/api/comments/1/reply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(replyPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parentId").value(1));

        mvc.perform(post("/api/resources/1/favorite"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.favorites").value(1));

        mvc.perform(post("/api/resources/1/like"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.likes").value(1));

        var reportPayload = mapper.writeValueAsString(Map.of("resourceId", "1", "reason", "spam"));
        mvc.perform(post("/api/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reportPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("OPEN"));
    }
}
