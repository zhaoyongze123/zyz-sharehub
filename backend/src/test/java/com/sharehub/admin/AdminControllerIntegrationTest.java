package com.sharehub.admin;

import com.sharehub.config.AdminTokenFilter;
import com.sharehub.interaction.InteractionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

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
    private InteractionRepository interactionRepository;

    @BeforeEach
    void cleanup() throws Exception {
        Files.deleteIfExists(Path.of("data/interaction.json"));
    }

    @Test
    void reportsSupportsPaginationAndFilters() throws Exception {
        interactionRepository.saveReport(1L, "spam", "alice");
        interactionRepository.saveReport(2L, "abuse", "bob");

        mvc.perform(adminGet("/api/admin/reports")
                .param("page", "1")
                .param("pageSize", "1")
                .param("status", "OPEN")
                .param("targetType", "RESOURCE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.total").value(2))
            .andExpect(jsonPath("$.data.items[0].targetType").value("RESOURCE"));
    }

    @Test
    void auditLogsFilterActionAndTarget() throws Exception {
        interactionRepository.appendAuditLog("BLOCK_RESOURCE", "RESOURCE", "42", "{}");
        interactionRepository.appendAuditLog("HIDE_COMMENT", "COMMENT", "7", "{}");

        mvc.perform(adminGet("/api/admin/audit-logs")
                .param("page", "1")
                .param("pageSize", "10")
                .param("action", "BLOCK_RESOURCE")
                .param("targetType", "RESOURCE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.items[0].action").value("BLOCK_RESOURCE"))
            .andExpect(jsonPath("$.data.items[0].targetType").value("RESOURCE"));
    }

    @Test
    void adminEndpointsRejectMissingToken() throws Exception {
        mvc.perform(get("/api/admin/reports"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value(AdminTokenFilter.ADMIN_TOKEN_REQUIRED));

        mvc.perform(get("/api/admin/audit-logs"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value(AdminTokenFilter.ADMIN_TOKEN_REQUIRED));
    }

    private MockHttpServletRequestBuilder adminGet(String uri) {
        return get(uri).header(AdminTokenFilter.HEADER, AdminTokenFilter.DEFAULT_ADMIN_TOKEN);
    }

    private MockHttpServletRequestBuilder adminPost(String uri) {
        return post(uri).header(AdminTokenFilter.HEADER, AdminTokenFilter.DEFAULT_ADMIN_TOKEN);
    }
}
