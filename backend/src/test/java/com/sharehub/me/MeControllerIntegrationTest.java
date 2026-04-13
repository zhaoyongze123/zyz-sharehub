package com.sharehub.me;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sharehub.auth.RequestAccessService;
import com.sharehub.auth.UserProfileDto;
import com.sharehub.auth.UserProfileRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MeControllerIntegrationTest {

    private static final String USER_KEY = "local-dev-user";

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
        jdbcTemplate.update("DELETE FROM favorites");
        jdbcTemplate.update("DELETE FROM note_view_history");
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
        String resourceResponse = mockMvc.perform(post("/api/resources")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"我的资料","type":"PDF","visibility":"PUBLIC"}
                    """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long resourceId = Long.valueOf(((Map<?, ?>) objectMapper.readValue(resourceResponse, Map.class).get("data")).get("id").toString());

        mockMvc.perform(post("/api/roadmaps")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"我的路线","description":"desc","visibility":"PUBLIC"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"我的笔记","contentMd":"# note","visibility":"PUBLIC","status":"DRAFT"}
                    """))
            .andExpect(status().isOk());

        String publishedNoteResponse = mockMvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"已发布笔记","contentMd":"# published","visibility":"PUBLIC","status":"PUBLISHED"}
                    """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long publishedNoteId = Long.valueOf(((Map<?, ?>) objectMapper.readValue(publishedNoteResponse, Map.class).get("data")).get("id").toString());

        mockMvc.perform(post("/api/resumes/generate")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("templateKey", "me"))))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/notes/" + publishedNoteId + "/favorite")
                .header(RequestAccessService.USER_KEY_HEADER, "another-user"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/me").header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.profile.login").value("local-dev-user"))
            .andExpect(jsonPath("$.data.myResourceCount").value(1))
            .andExpect(jsonPath("$.data.myFavoriteCount").value(1))
            .andExpect(jsonPath("$.data.myRoadmapCount").value(1))
            .andExpect(jsonPath("$.data.myNoteCount").value(2))
            .andExpect(jsonPath("$.data.myResumeCount").value(1))
            .andExpect(jsonPath("$.data.recentResourceCount").value(1))
            .andExpect(jsonPath("$.data.publishedResourceCount").value(0))
            .andExpect(jsonPath("$.data.draftNoteCount").value(1))
            .andExpect(jsonPath("$.data.generatedResumeCount").value(1));
    }

    @Test
    void shouldUpdatePersonalProfile() throws Exception {
        mockMvc.perform(put("/api/me/profile")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"displayName":"新昵称","bio":"真实个人简介"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.profile.login").value(USER_KEY))
            .andExpect(jsonPath("$.data.profile.name").value("新昵称"))
            .andExpect(jsonPath("$.data.profile.bio").value("真实个人简介"));

        mockMvc.perform(get("/api/me")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.profile.name").value("新昵称"))
            .andExpect(jsonPath("$.data.profile.bio").value("真实个人简介"));
    }

    @Test
    void shouldListPersonalWorkbenchCollections() throws Exception {
        String firstResourceResponse = mockMvc.perform(post("/api/resources")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"我的资料A","type":"PDF","visibility":"PUBLIC"}
                    """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long firstResourceId = Long.valueOf(((Map<?, ?>) objectMapper.readValue(firstResourceResponse, Map.class).get("data")).get("id").toString());

        mockMvc.perform(post("/api/resources")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"我的资料B","type":"DOC","visibility":"PRIVATE"}
                    """))
            .andExpect(status().isOk());

        String roadmapResponse = mockMvc.perform(post("/api/roadmaps")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"路线A","description":"desc-a","visibility":"PUBLIC","status":"PUBLISHED"}
                    """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long roadmapId = Long.valueOf(((Map<?, ?>) objectMapper.readValue(roadmapResponse, Map.class).get("data")).get("id").toString());

        mockMvc.perform(post("/api/roadmaps/" + roadmapId + "/nodes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"节点1","orderNo":1}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/roadmaps/" + roadmapId + "/nodes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"节点2","orderNo":2}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/roadmaps/" + roadmapId + "/progress")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"percent":50,"completedNodeIds":[1]}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"笔记A","contentMd":"# note-a","visibility":"PUBLIC","status":"PUBLISHED"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/resumes/generate")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("templateKey", "resume-a"))))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/resumes/generate")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("templateKey", "resume-b"))))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/resources/" + firstResourceId + "/favorite")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/me/resources")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .param("page", "1")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(2))
            .andExpect(jsonPath("$.data.items[0].title").value("我的资料B"));

        mockMvc.perform(get("/api/me/resources")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .param("status", "DRAFT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(2));

        mockMvc.perform(get("/api/me/resources")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .param("visibility", "PRIVATE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].title").value("我的资料B"));

        mockMvc.perform(get("/api/me/resources")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .param("status", " DRAFT ")
                .param("visibility", " PRIVATE "))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].title").value("我的资料B"));

        mockMvc.perform(get("/api/me/roadmaps")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .param("page", "1")
                .param("pageSize", "10")
                .param("status", "PUBLISHED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].title").value("路线A"))
            .andExpect(jsonPath("$.data.items[0].nodeCount").value(2))
            .andExpect(jsonPath("$.data.items[0].completedNodeCount").value(1))
            .andExpect(jsonPath("$.data.items[0].progressPercent").value(50));

        mockMvc.perform(get("/api/me/favorites")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .param("page", "1")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].title").value("我的资料A"));

        mockMvc.perform(get("/api/me/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .param("page", "1")
                .param("pageSize", "10")
                .param("status", "PUBLISHED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].title").value("笔记A"));

        mockMvc.perform(get("/api/me/resumes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .param("page", "1")
                .param("pageSize", "10")
                .param("status", "GENERATED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(2))
            .andExpect(jsonPath("$.data.items[0].fileSize").isNumber())
            .andExpect(jsonPath("$.data.items[0].fileCreatedAt").exists())
            .andExpect(jsonPath("$.data.items[0].fileUpdatedAt").exists());

        mockMvc.perform(get("/api/me/resumes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .param("templateKey", "resume-a")
                .param("page", "1")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].templateKey").value("resume-a"))
            .andExpect(jsonPath("$.data.items[0].fileName").value("resume-resume-a.pdf"));

        mockMvc.perform(get("/api/me/resumes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .param("keyword", "RESUME-B")
                .param("page", "1")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].templateKey").value("resume-b"));

        mockMvc.perform(get("/api/me/resumes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .param("status", " GENERATED ")
                .param("templateKey", " resume-a ")
                .param("keyword", " RESUME-A ")
                .param("page", "1")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].templateKey").value("resume-a"));

        mockMvc.perform(get("/api/me/resumes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .param("status", " ")
                .param("templateKey", "   ")
                .param("keyword", "\t")
                .param("page", "1")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(2))
            .andExpect(jsonPath("$.data.items[0].templateKey").value("resume-b"))
            .andExpect(jsonPath("$.data.items[1].templateKey").value("resume-a"));

        mockMvc.perform(get("/api/me").header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.myResourceCount").value(2))
            .andExpect(jsonPath("$.data.recentResourceCount").value(2))
            .andExpect(jsonPath("$.data.publishedResourceCount").value(0))
            .andExpect(jsonPath("$.data.draftNoteCount").value(0))
            .andExpect(jsonPath("$.data.generatedResumeCount").value(2));
    }

    @Test
    void shouldReturnFavoriteNotesAndPersistedNoteHistory() throws Exception {
        String noteResponse = mockMvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"闭环笔记","contentMd":"# title\\n真正的摘要内容","visibility":"PUBLIC","status":"PUBLISHED"}
                    """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long noteId = Long.valueOf(((Map<?, ?>) objectMapper.readValue(noteResponse, Map.class).get("data")).get("id").toString());

        mockMvc.perform(post("/api/notes/" + noteId + "/favorite")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.favorites").value(1));

        mockMvc.perform(get("/api/notes/" + noteId)
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(noteId));

        mockMvc.perform(get("/api/me/favorite-notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .param("page", "1")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].id").value(noteId))
            .andExpect(jsonPath("$.data.items[0].title").value("闭环笔记"))
            .andExpect(jsonPath("$.data.items[0].favorited").value(true))
            .andExpect(jsonPath("$.data.items[0].favorites").value(1));

        mockMvc.perform(get("/api/me/note-history")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .param("page", "1")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].id").value(noteId))
            .andExpect(jsonPath("$.data.items[0].title").value("闭环笔记"))
            .andExpect(jsonPath("$.data.items[0].favorited").value(true))
            .andExpect(jsonPath("$.data.items[0].favorites").value(1));
    }

    @Test
    void shouldTreatBlankNoteStatusFilterAsUnfiltered() throws Exception {
        mockMvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"笔记A","contentMd":"# note-a","visibility":"PUBLIC","status":"DRAFT"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"笔记B","contentMd":"# note-b","visibility":"PRIVATE","status":"PUBLISHED"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/me/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .param("status", "   ")
                .param("page", "1")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(2))
            .andExpect(jsonPath("$.data.items[0].title").value("笔记B"))
            .andExpect(jsonPath("$.data.items[1].title").value("笔记A"));
    }

    @Test
    void shouldTreatBlankNoteStatusAsUnfilteredAndClampInvalidPagination() throws Exception {
        mockMvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"笔记A","contentMd":"# note-a","visibility":"PUBLIC","status":"DRAFT"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"笔记B","contentMd":"# note-b","visibility":"PRIVATE","status":"PUBLISHED"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/me/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .param("status", "\t")
                .param("page", "0")
                .param("pageSize", "-2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(2))
            .andExpect(jsonPath("$.data.page").value(1))
            .andExpect(jsonPath("$.data.pageSize").value(1))
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.items[0].title").value("笔记B"));
    }

    @Test
    void shouldTrimPersonalCenterNoteStatusFilterBeforeApplying() throws Exception {
        mockMvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"笔记A","contentMd":"# note-a","visibility":"PUBLIC","status":"DRAFT"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"笔记B","contentMd":"# note-b","visibility":"PRIVATE","status":"PUBLISHED"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/me/notes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .param("status", " PUBLISHED ")
                .param("page", "1")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].title").value("笔记B"))
            .andExpect(jsonPath("$.data.items[0].status").value("PUBLISHED"));
    }

    @Test
    void shouldClampInvalidResumePaginationToMinimumValues() throws Exception {
        mockMvc.perform(post("/api/resumes/generate")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("templateKey", "resume-a"))))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/resumes/generate")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("templateKey", "resume-b"))))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/me/resumes")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .param("page", "0")
                .param("pageSize", "-3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(2))
            .andExpect(jsonPath("$.data.page").value(1))
            .andExpect(jsonPath("$.data.pageSize").value(1))
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.items[0].templateKey").value("resume-b"));
    }

    @Test
    void shouldTreatBlankResourceFiltersAsUnfilteredAndClampInvalidPagination() throws Exception {
        mockMvc.perform(post("/api/resources")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"资料A","type":"PDF","visibility":"PUBLIC","status":"PUBLISHED"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/resources")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"资料B","type":"DOC","visibility":"PRIVATE","status":"DRAFT"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/me/resources")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .param("status", "   ")
                .param("visibility", "\t")
                .param("page", "0")
                .param("pageSize", "-2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(2))
            .andExpect(jsonPath("$.data.page").value(1))
            .andExpect(jsonPath("$.data.pageSize").value(1))
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.items[0].title").value("资料B"));
    }

    @Test
    void shouldTreatBlankRoadmapStatusAsUnfilteredAndClampInvalidPagination() throws Exception {
        mockMvc.perform(post("/api/roadmaps")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"路线A","description":"desc-a","visibility":"PUBLIC","status":"PUBLISHED"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/roadmaps")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"路线B","description":"desc-b","visibility":"PRIVATE","status":"DRAFT"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/me/roadmaps")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .param("status", " ")
                .param("page", "-1")
                .param("pageSize", "0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(2))
            .andExpect(jsonPath("$.data.page").value(1))
            .andExpect(jsonPath("$.data.pageSize").value(1))
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.items[0].title").value("路线B"));
    }

    @Test
    void shouldClampInvalidFavoritePaginationToMinimumValues() throws Exception {
        String firstResourceResponse = mockMvc.perform(post("/api/resources")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"收藏资料A","type":"PDF","visibility":"PUBLIC"}
                    """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long firstResourceId = Long.valueOf(((Map<?, ?>) objectMapper.readValue(firstResourceResponse, Map.class).get("data")).get("id").toString());

        String secondResourceResponse = mockMvc.perform(post("/api/resources")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"收藏资料B","type":"DOC","visibility":"PRIVATE"}
                    """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long secondResourceId = Long.valueOf(((Map<?, ?>) objectMapper.readValue(secondResourceResponse, Map.class).get("data")).get("id").toString());

        mockMvc.perform(post("/api/resources/" + firstResourceId + "/favorite")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/resources/" + secondResourceId + "/favorite")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/me/favorites")
                .header(RequestAccessService.USER_KEY_HEADER, USER_KEY)
                .param("page", "0")
                .param("pageSize", "-5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(2))
            .andExpect(jsonPath("$.data.page").value(1))
            .andExpect(jsonPath("$.data.pageSize").value(1))
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.items[0].title").value("收藏资料B"));
    }

    @Test
    void shouldRejectAnonymousAccessToPersonalCenter() throws Exception {
        mockMvc.perform(get("/api/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NOT_LOGGED_IN"));

        mockMvc.perform(get("/api/me/resources"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NOT_LOGGED_IN"));

        mockMvc.perform(get("/api/me/roadmaps"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NOT_LOGGED_IN"));

        mockMvc.perform(get("/api/me/favorites"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NOT_LOGGED_IN"));

        mockMvc.perform(get("/api/me/favorite-notes"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NOT_LOGGED_IN"));

        mockMvc.perform(get("/api/me/notes"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NOT_LOGGED_IN"));

        mockMvc.perform(get("/api/me/note-history"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NOT_LOGGED_IN"));

        mockMvc.perform(get("/api/me/resumes"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NOT_LOGGED_IN"));
    }

    @Test
    void shouldRejectBannedAccessToPersonalCenter() throws Exception {
        UserProfileDto bannedUser = userProfileRepository.upsert("me-banned-user", "me-banned-user", null);
        userProfileRepository.updateStatus(bannedUser.id(), "BANNED");

        mockMvc.perform(get("/api/me")
                .header(RequestAccessService.USER_KEY_HEADER, bannedUser.login()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("USER_BANNED"))
            .andExpect(jsonPath("$.message").value("USER_BANNED"));

        mockMvc.perform(get("/api/me/resources")
                .header(RequestAccessService.USER_KEY_HEADER, bannedUser.login()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("USER_BANNED"))
            .andExpect(jsonPath("$.message").value("USER_BANNED"));

        mockMvc.perform(get("/api/me/roadmaps")
                .header(RequestAccessService.USER_KEY_HEADER, bannedUser.login()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("USER_BANNED"))
            .andExpect(jsonPath("$.message").value("USER_BANNED"));

        mockMvc.perform(get("/api/me/favorites")
                .header(RequestAccessService.USER_KEY_HEADER, bannedUser.login()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("USER_BANNED"))
            .andExpect(jsonPath("$.message").value("USER_BANNED"));

        mockMvc.perform(get("/api/me/favorite-notes")
                .header(RequestAccessService.USER_KEY_HEADER, bannedUser.login()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("USER_BANNED"))
            .andExpect(jsonPath("$.message").value("USER_BANNED"));

        mockMvc.perform(get("/api/me/notes")
                .header(RequestAccessService.USER_KEY_HEADER, bannedUser.login()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("USER_BANNED"))
            .andExpect(jsonPath("$.message").value("USER_BANNED"));

        mockMvc.perform(get("/api/me/note-history")
                .header(RequestAccessService.USER_KEY_HEADER, bannedUser.login()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("USER_BANNED"))
            .andExpect(jsonPath("$.message").value("USER_BANNED"));

        mockMvc.perform(get("/api/me/resumes")
                .header(RequestAccessService.USER_KEY_HEADER, bannedUser.login()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("USER_BANNED"))
            .andExpect(jsonPath("$.message").value("USER_BANNED"));
    }

    @Test
    void shouldAutoProvisionUserWhenFirstAccessingPersonalCenter() throws Exception {
        String firstAccessUser = "fresh-me-user";

        mockMvc.perform(get("/api/me")
                .header(RequestAccessService.USER_KEY_HEADER, firstAccessUser))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.profile.login").value(firstAccessUser))
            .andExpect(jsonPath("$.data.profile.status").value("ACTIVE"))
            .andExpect(jsonPath("$.data.myResourceCount").value(0))
            .andExpect(jsonPath("$.data.myResumeCount").value(0));

        mockMvc.perform(get("/api/me/resumes")
                .header(RequestAccessService.USER_KEY_HEADER, firstAccessUser))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(get("/api/me/resources")
                .header(RequestAccessService.USER_KEY_HEADER, firstAccessUser))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(get("/api/me/roadmaps")
                .header(RequestAccessService.USER_KEY_HEADER, firstAccessUser))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(get("/api/me/favorites")
                .header(RequestAccessService.USER_KEY_HEADER, firstAccessUser))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(get("/api/me/favorite-notes")
                .header(RequestAccessService.USER_KEY_HEADER, firstAccessUser))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(get("/api/me/notes")
                .header(RequestAccessService.USER_KEY_HEADER, firstAccessUser))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(get("/api/me/note-history")
                .header(RequestAccessService.USER_KEY_HEADER, firstAccessUser))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(0));

        Integer userCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE login = ? AND status = 'ACTIVE'",
            Integer.class,
            firstAccessUser
        );
        org.assertj.core.api.Assertions.assertThat(userCount).isEqualTo(1);
    }
}
