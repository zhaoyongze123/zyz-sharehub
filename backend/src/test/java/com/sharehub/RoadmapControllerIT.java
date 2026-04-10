package com.sharehub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class RoadmapControllerIT {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private ObjectMapper objectMapper;

  @BeforeEach
  void cleanup() throws Exception {
    // ensure database schema exists before tests run (roadmap repository handles DDL)
    mvc.perform(get("/api/roadmaps?page=1&pageSize=1"));
  }

  @Test
  void listSupportsPagination() throws Exception {
    createRoadmap("Agent 1");
    createRoadmap("Agent 2");

    MvcResult result =
        mvc.perform(get("/api/roadmaps").param("page", "1").param("pageSize", "1"))
            .andReturn();

    JsonNode node =
        objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    JsonNode data = node.get("data");
    assertThat(data.get("total").asInt()).isEqualTo(2);
    assertThat(data.get("page").asInt()).isEqualTo(1);
    assertThat(data.get("pageSize").asInt()).isEqualTo(1);
    assertThat(data.get("items")).hasSize(1);
  }

  @Test
  void detailIncludesNodesAndProgress() throws Exception {
    long roadmapId = createRoadmap("Agentic Journey");
    addNode(roadmapId, "阶段 1", null, 1);
    Map<String, Object> payload = Map.of("percent", 50, "completedNodeIds", new long[] {1});
    mvc.perform(
            post("/api/roadmaps/" + roadmapId + "/progress")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
        .andReturn();

    MvcResult detail =
        mvc.perform(get("/api/roadmaps/" + roadmapId)).andReturn();
    JsonNode node =
        objectMapper.readTree(detail.getResponse().getContentAsString(StandardCharsets.UTF_8));
    JsonNode responseData = node.get("data");
    assertThat(responseData.get("roadmap").get("title").asText()).isEqualTo("Agentic Journey");
    assertThat(responseData.get("nodes")).hasSize(1);
    assertThat(responseData.get("progress").get("percent").asInt()).isEqualTo(50);
  }

  @Test
  void progressUpdateOverridesPreviousPayload() throws Exception {
    long roadmapId = createRoadmap("Progress Replay");

    mvc.perform(
            post("/api/roadmaps/" + roadmapId + "/progress")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("percent", 10))))
        .andReturn();

    mvc.perform(
            post("/api/roadmaps/" + roadmapId + "/progress")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("percent", 95))))
        .andReturn();

    MvcResult detail = mvc.perform(get("/api/roadmaps/" + roadmapId)).andReturn();
    JsonNode node =
        objectMapper.readTree(detail.getResponse().getContentAsString(StandardCharsets.UTF_8));
    JsonNode progress = node.get("data").get("progress");
    assertThat(progress.get("percent").asInt()).isEqualTo(95);
  }

  private long createRoadmap(String title) throws Exception {
    JsonNode payload =
        objectMapper.readTree(
            mvc.perform(
                    post("/api/roadmaps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsString(
                                Map.of("title", title, "description", "desc", "visibility", "PUBLIC"))))
                .andReturn()
                .getResponse()
                .getContentAsString());
    return payload.get("data").get("id").asLong();
  }

  private void addNode(long roadmapId, String title, Long parent, int orderNo) throws Exception {
    mvc.perform(
            post("/api/roadmaps/" + roadmapId + "/nodes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("parentId", parent, "title", title, "orderNo", orderNo))))
        .andReturn();
  }
}
