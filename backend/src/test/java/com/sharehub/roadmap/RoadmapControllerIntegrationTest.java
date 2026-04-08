package com.sharehub.roadmap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sharehub.auth.RequestAccessService;
import com.sharehub.auth.UserProfileDto;
import com.sharehub.auth.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoadmapControllerIntegrationTest {

    private static final String OWNER = "roadmap-owner";
    private static final String OTHER_USER = "roadmap-other";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM roadmap_progress");
        jdbcTemplate.update("DELETE FROM roadmap_nodes");
        jdbcTemplate.update("DELETE FROM roadmaps");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void shouldIsolateRoadmapOwnerAndProgressByRequestUser() throws Exception {
        String createResponse = mockMvc.perform(post("/api/roadmaps")
                .header(RequestAccessService.USER_KEY_HEADER, OWNER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"路线A","description":"desc","visibility":"PUBLIC","status":"PUBLISHED"}
                    """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long roadmapId = Long.valueOf(String.valueOf(((Map<?, ?>) objectMapper.readValue(createResponse, Map.class).get("data")).get("id")));

        mockMvc.perform(post("/api/roadmaps/" + roadmapId + "/nodes")
                .header(RequestAccessService.USER_KEY_HEADER, OWNER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"节点1","orderNo":1}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].title").value("节点1"));

        mockMvc.perform(post("/api/roadmaps/" + roadmapId + "/progress")
                .header(RequestAccessService.USER_KEY_HEADER, OWNER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"percent":75,"completedNodeIds":[1]}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.percent").value(75));

        mockMvc.perform(get("/api/me/roadmaps")
                .header(RequestAccessService.USER_KEY_HEADER, OWNER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].title").value("路线A"))
            .andExpect(jsonPath("$.data.items[0].progressPercent").value(75));

        mockMvc.perform(get("/api/me/roadmaps")
                .header(RequestAccessService.USER_KEY_HEADER, OTHER_USER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(get("/api/roadmaps/" + roadmapId)
                .header(RequestAccessService.USER_KEY_HEADER, OWNER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.progress.percent").value(75))
            .andExpect(jsonPath("$.data.nodes[0].title").value("节点1"));

        mockMvc.perform(get("/api/roadmaps/" + roadmapId)
                .header(RequestAccessService.USER_KEY_HEADER, OTHER_USER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.progress").isMap())
            .andExpect(jsonPath("$.data.progress.percent").doesNotExist())
            .andExpect(jsonPath("$.data.nodes[0].title").value("节点1"));

        mockMvc.perform(post("/api/roadmaps/" + roadmapId + "/nodes")
                .header(RequestAccessService.USER_KEY_HEADER, OTHER_USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"越权节点","orderNo":2}
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("ROADMAP_NOT_FOUND"));

        mockMvc.perform(post("/api/roadmaps/" + roadmapId + "/progress")
                .header(RequestAccessService.USER_KEY_HEADER, OTHER_USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"percent":100}
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("ROADMAP_NOT_FOUND"));
    }

    @Test
    void shouldRejectAnonymousAccessToRoadmapMutations() throws Exception {
        mockMvc.perform(post("/api/roadmaps")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"路线A","description":"desc","visibility":"PUBLIC","status":"PUBLISHED"}
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NOT_LOGGED_IN"));

        String createResponse = mockMvc.perform(post("/api/roadmaps")
                .header(RequestAccessService.USER_KEY_HEADER, OWNER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"路线A","description":"desc","visibility":"PUBLIC","status":"PUBLISHED"}
                    """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long roadmapId = Long.valueOf(String.valueOf(((Map<?, ?>) objectMapper.readValue(createResponse, Map.class).get("data")).get("id")));

        mockMvc.perform(post("/api/roadmaps/" + roadmapId + "/nodes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"节点1","orderNo":1}
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NOT_LOGGED_IN"));

        mockMvc.perform(post("/api/roadmaps/" + roadmapId + "/progress")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"percent":75}
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NOT_LOGGED_IN"));
    }

    @Test
    void shouldRejectBannedUserAccessToRoadmapMutations() throws Exception {
        UserProfileDto bannedUser = userProfileRepository.upsert("roadmap-banned-user", "roadmap-banned-user", null);
        userProfileRepository.updateStatus(bannedUser.id(), "BANNED");

        mockMvc.perform(post("/api/roadmaps")
                .header(RequestAccessService.USER_KEY_HEADER, bannedUser.login())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"路线A","description":"desc","visibility":"PUBLIC","status":"PUBLISHED"}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("USER_BANNED"));

        String createResponse = mockMvc.perform(post("/api/roadmaps")
                .header(RequestAccessService.USER_KEY_HEADER, OWNER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"路线A","description":"desc","visibility":"PUBLIC","status":"PUBLISHED"}
                    """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long roadmapId = Long.valueOf(String.valueOf(((Map<?, ?>) objectMapper.readValue(createResponse, Map.class).get("data")).get("id")));

        mockMvc.perform(post("/api/roadmaps/" + roadmapId + "/nodes")
                .header(RequestAccessService.USER_KEY_HEADER, bannedUser.login())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"节点1","orderNo":1}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("USER_BANNED"));

        mockMvc.perform(post("/api/roadmaps/" + roadmapId + "/progress")
                .header(RequestAccessService.USER_KEY_HEADER, bannedUser.login())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"percent":75}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("USER_BANNED"));
    }
}
