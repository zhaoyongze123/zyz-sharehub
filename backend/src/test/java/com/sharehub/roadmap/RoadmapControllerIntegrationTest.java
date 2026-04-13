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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
        jdbcTemplate.update("DELETE FROM resumes");
        jdbcTemplate.update("DELETE FROM files");
        jdbcTemplate.update("DELETE FROM roadmap_node_progress");
        jdbcTemplate.update("DELETE FROM roadmap_enrollments");
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
                    {"title":"节点1","description":"节点1描述","orderNo":1}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].title").value("节点1"));

        Long nodeId = jdbcTemplate.queryForObject(
            "SELECT id FROM roadmap_nodes WHERE roadmap_id = ? AND order_no = 1",
            Long.class,
            roadmapId
        );

        mockMvc.perform(multipart("/api/roadmaps/{id}/nodes/{nodeId}/attachments", roadmapId, nodeId)
                .file(new MockMultipartFile("file", "node-1.txt", MediaType.TEXT_PLAIN_VALUE, "roadmap-node-file".getBytes()))
                .header(RequestAccessService.USER_KEY_HEADER, OWNER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.filename").value("node-1.txt"))
            .andExpect(jsonPath("$.data.downloadUrl").exists());

        mockMvc.perform(post("/api/roadmaps/" + roadmapId + "/progress")
                .header(RequestAccessService.USER_KEY_HEADER, OWNER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"percent":75,"completedNodeIds":[%d]}
                    """.formatted(nodeId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.percent").value(75));

        Integer completedNodeProgressCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM roadmap_node_progress WHERE roadmap_id = ? AND user_key = ? AND status = 'COMPLETED'",
            Integer.class,
            roadmapId,
            OWNER
        );
        org.assertj.core.api.Assertions.assertThat(completedNodeProgressCount).isEqualTo(1);

        mockMvc.perform(get("/api/me/authored-roadmaps")
                .header(RequestAccessService.USER_KEY_HEADER, OWNER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].title").value("路线A"))
            .andExpect(jsonPath("$.data.items[0].progressPercent").value(75))
            .andExpect(jsonPath("$.data.items[0].enrollmentStatus").isEmpty());

        mockMvc.perform(get("/api/me/roadmaps")
                .header(RequestAccessService.USER_KEY_HEADER, OTHER_USER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(get("/api/roadmaps/" + roadmapId)
                .header(RequestAccessService.USER_KEY_HEADER, OWNER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.progress.percent").value(75))
            .andExpect(jsonPath("$.data.nodes[0].title").value("节点1"))
            .andExpect(jsonPath("$.data.nodes[0].description").value("节点1描述"))
            .andExpect(jsonPath("$.data.nodes[0].attachments[0].filename").value("node-1.txt"));

        mockMvc.perform(get("/api/roadmaps/" + roadmapId)
                .header(RequestAccessService.USER_KEY_HEADER, OTHER_USER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.progress").isMap())
            .andExpect(jsonPath("$.data.progress.percent").doesNotExist())
            .andExpect(jsonPath("$.data.enrollment").isEmpty())
            .andExpect(jsonPath("$.data.nodes[0].title").value("节点1"));

        mockMvc.perform(post("/api/roadmaps/" + roadmapId + "/enrollment")
                .header(RequestAccessService.USER_KEY_HEADER, OTHER_USER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.roadmapId").value(roadmapId))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(get("/api/me/roadmaps")
                .header(RequestAccessService.USER_KEY_HEADER, OTHER_USER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].title").value("路线A"))
            .andExpect(jsonPath("$.data.items[0].enrollmentStatus").value("ACTIVE"));

        mockMvc.perform(get("/api/roadmaps/" + roadmapId)
                .header(RequestAccessService.USER_KEY_HEADER, OTHER_USER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.enrollment.status").value("ACTIVE"));

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

        mockMvc.perform(multipart("/api/roadmaps/{id}/nodes/{nodeId}/attachments", roadmapId, nodeId)
                .file(new MockMultipartFile("file", "deny.txt", MediaType.TEXT_PLAIN_VALUE, "deny".getBytes()))
                .header(RequestAccessService.USER_KEY_HEADER, OTHER_USER))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("ROADMAP_NOT_FOUND"));
    }

    @Test
    void enrollmentLifecycleShouldBeIdempotentAndQueryable() throws Exception {
        String createResponse = mockMvc.perform(post("/api/roadmaps")
                .header(RequestAccessService.USER_KEY_HEADER, OWNER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"路线生命周期","description":"desc","visibility":"PUBLIC","status":"PUBLISHED"}
                    """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long roadmapId = Long.valueOf(String.valueOf(((Map<?, ?>) objectMapper.readValue(createResponse, Map.class).get("data")).get("id")));

        mockMvc.perform(post("/api/roadmaps/" + roadmapId + "/enrollment")
                .header(RequestAccessService.USER_KEY_HEADER, OTHER_USER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(post("/api/roadmaps/" + roadmapId + "/enrollment")
                .header(RequestAccessService.USER_KEY_HEADER, OTHER_USER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(post("/api/roadmaps/" + roadmapId + "/enrollment/pause")
                .header(RequestAccessService.USER_KEY_HEADER, OTHER_USER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PAUSED"));

        mockMvc.perform(post("/api/roadmaps/" + roadmapId + "/enrollment/resume")
                .header(RequestAccessService.USER_KEY_HEADER, OTHER_USER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(post("/api/roadmaps/" + roadmapId + "/enrollment/complete")
                .header(RequestAccessService.USER_KEY_HEADER, OTHER_USER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("COMPLETED"))
            .andExpect(jsonPath("$.data.completedAt").exists());

        mockMvc.perform(get("/api/roadmaps/" + roadmapId + "/enrollment")
                .header(RequestAccessService.USER_KEY_HEADER, OTHER_USER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void shouldSyncNodeProgressAndSupportRevokingCompletedNodes() throws Exception {
        String createResponse = mockMvc.perform(post("/api/roadmaps")
                .header(RequestAccessService.USER_KEY_HEADER, OWNER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"路线进度同步","description":"desc","visibility":"PUBLIC","status":"PUBLISHED"}
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
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/roadmaps/" + roadmapId + "/nodes")
                .header(RequestAccessService.USER_KEY_HEADER, OWNER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"节点2","orderNo":2}
                    """))
            .andExpect(status().isOk());

        Long firstNodeId = jdbcTemplate.queryForObject(
            "SELECT id FROM roadmap_nodes WHERE roadmap_id = ? AND order_no = 1",
            Long.class,
            roadmapId
        );
        Long secondNodeId = jdbcTemplate.queryForObject(
            "SELECT id FROM roadmap_nodes WHERE roadmap_id = ? AND order_no = 2",
            Long.class,
            roadmapId
        );

        mockMvc.perform(post("/api/roadmaps/" + roadmapId + "/progress")
                .header(RequestAccessService.USER_KEY_HEADER, OWNER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"completedNodeIds":[%d,%d]}
                    """.formatted(firstNodeId, secondNodeId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.percent").value(100))
            .andExpect(jsonPath("$.data.completedNodeIds.length()").value(2));

        mockMvc.perform(post("/api/roadmaps/" + roadmapId + "/progress")
                .header(RequestAccessService.USER_KEY_HEADER, OWNER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"completedNodeIds":[%d]}
                    """.formatted(firstNodeId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.percent").value(50))
            .andExpect(jsonPath("$.data.completedNodeIds.length()").value(1));

        mockMvc.perform(get("/api/roadmaps/" + roadmapId)
                .header(RequestAccessService.USER_KEY_HEADER, OWNER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.progress.percent").value(50))
            .andExpect(jsonPath("$.data.progress.completedNodeIds.length()").value(1));

        mockMvc.perform(get("/api/me/authored-roadmaps")
                .header(RequestAccessService.USER_KEY_HEADER, OWNER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].completedNodeCount").value(1))
            .andExpect(jsonPath("$.data.items[0].progressPercent").value(50));

        String firstNodeStatus = jdbcTemplate.queryForObject(
            "SELECT status FROM roadmap_node_progress WHERE roadmap_id = ? AND node_id = ? AND user_key = ?",
            String.class,
            roadmapId,
            firstNodeId,
            OWNER
        );
        String secondNodeStatus = jdbcTemplate.queryForObject(
            "SELECT status FROM roadmap_node_progress WHERE roadmap_id = ? AND node_id = ? AND user_key = ?",
            String.class,
            roadmapId,
            secondNodeId,
            OWNER
        );
        org.assertj.core.api.Assertions.assertThat(firstNodeStatus).isEqualTo("COMPLETED");
        org.assertj.core.api.Assertions.assertThat(secondNodeStatus).isEqualTo("NOT_STARTED");
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

        mockMvc.perform(multipart("/api/roadmaps/{id}/nodes/{nodeId}/attachments", roadmapId, 1L)
                .file(new MockMultipartFile("file", "node.txt", MediaType.TEXT_PLAIN_VALUE, "node".getBytes())))
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

        mockMvc.perform(multipart("/api/roadmaps/{id}/nodes/{nodeId}/attachments", roadmapId, 1L)
                .file(new MockMultipartFile("file", "node.txt", MediaType.TEXT_PLAIN_VALUE, "node".getBytes()))
                .header(RequestAccessService.USER_KEY_HEADER, bannedUser.login()))
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

    @Test
    void shouldRejectBannedUserAccessToRoadmapDetailWhenIdentityProvided() throws Exception {
        UserProfileDto bannedUser = userProfileRepository.upsert("roadmap-banned-user", "roadmap-banned-user", null);
        userProfileRepository.updateStatus(bannedUser.id(), "BANNED");

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

        mockMvc.perform(get("/api/roadmaps/" + roadmapId)
                .header(RequestAccessService.USER_KEY_HEADER, bannedUser.login()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("USER_BANNED"));

        mockMvc.perform(get("/api/roadmaps/" + roadmapId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.progress").isMap())
            .andExpect(jsonPath("$.data.progress.percent").doesNotExist());
    }

    @Test
    void shouldClampInvalidPublicRoadmapPaginationAndDefaultStatusOnCreate() throws Exception {
        mockMvc.perform(post("/api/roadmaps")
                .header(RequestAccessService.USER_KEY_HEADER, OWNER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"路线A","description":"desc-a","visibility":"PUBLIC"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        mockMvc.perform(post("/api/roadmaps")
                .header(RequestAccessService.USER_KEY_HEADER, OWNER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"路线B","description":"desc-b","visibility":"PRIVATE","status":"DRAFT"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/roadmaps")
                .param("page", "0")
                .param("pageSize", "-2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(2))
            .andExpect(jsonPath("$.data.page").value(1))
            .andExpect(jsonPath("$.data.pageSize").value(1))
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.items[0].title").value("路线B"));
    }

    @Test
    void shouldTrimOrDefaultRoadmapStatusOnCreate() throws Exception {
        mockMvc.perform(post("/api/roadmaps")
                .header(RequestAccessService.USER_KEY_HEADER, OWNER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"路线A","description":"desc-a","visibility":"PUBLIC","status":" DRAFT "}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("DRAFT"));

        mockMvc.perform(post("/api/roadmaps")
                .header(RequestAccessService.USER_KEY_HEADER, OWNER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"路线B","description":"desc-b","visibility":"PUBLIC","status":"   "}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        mockMvc.perform(get("/api/roadmaps")
                .param("page", "1")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(2))
            .andExpect(jsonPath("$.data.items[0].title").value("路线B"))
            .andExpect(jsonPath("$.data.items[0].status").value("PUBLISHED"))
            .andExpect(jsonPath("$.data.items[1].title").value("路线A"))
            .andExpect(jsonPath("$.data.items[1].status").value("DRAFT"));
    }

    @Test
    void shouldSoftDeleteRoadmapAndHideItFromDefaultQueries() throws Exception {
        String createResponse = mockMvc.perform(post("/api/roadmaps")
                .header(RequestAccessService.USER_KEY_HEADER, OWNER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"待删除路线","description":"desc","visibility":"PUBLIC","status":"PUBLISHED"}
                    """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long roadmapId = Long.valueOf(String.valueOf(((Map<?, ?>) objectMapper.readValue(createResponse, Map.class).get("data")).get("id")));

        mockMvc.perform(delete("/api/roadmaps/" + roadmapId)
                .header(RequestAccessService.USER_KEY_HEADER, OWNER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("DELETED"));

        Map<String, Object> deletedRow = jdbcTemplate.queryForMap(
            "SELECT deleted_at, deleted_by FROM roadmaps WHERE id = ?",
            roadmapId
        );
        org.assertj.core.api.Assertions.assertThat(deletedRow.get("deleted_at")).isNotNull();
        org.assertj.core.api.Assertions.assertThat(deletedRow.get("deleted_by")).isEqualTo(OWNER);

        mockMvc.perform(get("/api/roadmaps")
                .param("page", "1")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(get("/api/roadmaps/" + roadmapId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("ROADMAP_NOT_FOUND"));
    }
}
