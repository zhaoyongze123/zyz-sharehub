package com.sharehub.note;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class NoteControllerIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void createListDetailUpdateDelete() throws Exception {
        NoteDto payload = new NoteDto(null, "Test", "# content", "PUBLIC", "DRAFT");
        String body = mapper.writeValueAsString(payload);

        String response = mvc.perform(post("/api/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();

        Map<?, ?> created = mapper.readValue(response, Map.class);
        Map<?, ?> data = (Map<?, ?>) created.get("data");
        Long id = Long.valueOf(data.get("id").toString());

        mvc.perform(get("/api/notes")
                .param("page", "1")
                .param("pageSize", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items").isArray())
            .andExpect(jsonPath("$.data.total").value(1));

        mvc.perform(get("/api/notes/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("Test"));

        payload = new NoteDto(id, "Updated", "# new", "PUBLIC", "PUBLISHED");
        mvc.perform(put("/api/notes/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        mvc.perform(delete("/api/notes/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("DELETED"));
    }
}
