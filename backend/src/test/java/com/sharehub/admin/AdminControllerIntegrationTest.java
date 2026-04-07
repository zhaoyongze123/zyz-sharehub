package com.sharehub.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
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
public class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void listAndResolveReports() throws Exception {
        var payload = mapper.writeValueAsString(Map.of("resourceId", "42", "reason", "spam"));
        String response = mvc.perform(post("/api/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Map<?, ?> created = mapper.readValue(response, Map.class);
        Map<?, ?> data = (Map<?, ?>) created.get("data");
        Long reportId = Long.valueOf(data.get("id").toString());

        mvc.perform(get("/api/admin/reports"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].status").value("OPEN"));

        mvc.perform(post("/api/admin/reports/" + reportId + "/resolve"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("RESOLVED"));

        mvc.perform(get("/api/admin/audit-logs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].action").exists());
    }

    @Test
    void shouldBanAndUnbanUser() throws Exception {
        String meResponse = mvc.perform(get("/api/auth/me"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Map<?, ?> payload = mapper.readValue(meResponse, Map.class);
        Map<?, ?> data = (Map<?, ?>) payload.get("data");
        Long userId = Long.valueOf(String.valueOf(data.get("id")));

        mvc.perform(post("/api/admin/users/" + userId + "/ban"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("BANNED"));

        mvc.perform(get("/api/auth/me"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("USER_BANNED"));

        mvc.perform(get("/api/me"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("USER_BANNED"));

        mvc.perform(post("/api/admin/users/" + userId + "/unban"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mvc.perform(get("/api/auth/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void shouldBlockAndRestoreResource() throws Exception {
        String createResponse = mvc.perform(post("/api/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"治理资料","type":"PDF","visibility":"PUBLIC"}
                    """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Map<?, ?> payload = mapper.readValue(createResponse, Map.class);
        Map<?, ?> data = (Map<?, ?>) payload.get("data");
        Long resourceId = Long.valueOf(String.valueOf(data.get("id")));

        mvc.perform(post("/api/resources/" + resourceId + "/publish"))
            .andExpect(status().isOk());

        mvc.perform(post("/api/admin/resources/" + resourceId + "/block"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("REMOVED"));

        mvc.perform(get("/api/resources/" + resourceId))
            .andExpect(status().isGone())
            .andExpect(jsonPath("$.message").value("RESOURCE_REMOVED"));

        mvc.perform(get("/api/resources"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[?(@.id==" + resourceId + ")]").isEmpty());

        mvc.perform(post("/api/admin/resources/" + resourceId + "/restore"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        mvc.perform(get("/api/resources/" + resourceId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(resourceId.intValue()));
    }

    @Test
    void shouldResolveReportAndAutoRemoveResource() throws Exception {
        String createResponse = mvc.perform(post("/api/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"举报资料","type":"PDF","visibility":"PUBLIC"}
                    """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Map<?, ?> resourcePayload = mapper.readValue(createResponse, Map.class);
        Long resourceId = Long.valueOf(String.valueOf(((Map<?, ?>) resourcePayload.get("data")).get("id")));

        mvc.perform(post("/api/resources/" + resourceId + "/publish"))
            .andExpect(status().isOk());

        String reportResponse = mvc.perform(post("/api/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("resourceId", String.valueOf(resourceId), "reason", "spam"))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Map<?, ?> reportPayload = mapper.readValue(reportResponse, Map.class);
        Long reportId = Long.valueOf(String.valueOf(((Map<?, ?>) reportPayload.get("data")).get("id")));

        mvc.perform(post("/api/admin/reports/" + reportId + "/resolve"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("RESOLVED"));

        mvc.perform(get("/api/resources/" + resourceId))
            .andExpect(status().isGone())
            .andExpect(jsonPath("$.message").value("RESOURCE_REMOVED"));

        mvc.perform(get("/api/admin/audit-logs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].action").value("AUTO_REMOVE_RESOURCE"));
    }
}
