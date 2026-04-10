package com.sharehub.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    properties = {
        "sharehub.admin.dev-token-enabled=false",
        "sharehub.admin.token=dev-admin-token"
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminTokenFilterProdModeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRejectAdminTokenWhenDevTokenModeDisabled() throws Exception {
        mockMvc.perform(get("/api/admin/reports").header(AdminTokenFilter.HEADER, "dev-admin-token"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value(AdminTokenFilter.ADMIN_TOKEN_REQUIRED));
    }
}
