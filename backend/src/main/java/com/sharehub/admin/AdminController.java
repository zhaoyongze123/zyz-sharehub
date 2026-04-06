package com.sharehub.admin;

import com.sharehub.common.ApiResponse;
import com.sharehub.common.InMemoryStore;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final InMemoryStore store;

    public AdminController(InMemoryStore store) {
        this.store = store;
    }

    @GetMapping("/reports")
    public ApiResponse<List<Object>> reports() {
        return ApiResponse.ok(new ArrayList<>(store.reports.values()));
    }

    @PostMapping("/reports/{id}/resolve")
    public ApiResponse<String> resolve(@PathVariable Long id) {
        Object found = store.reports.get(id);
        if (found instanceof java.util.Map<?, ?> report) {
            ((java.util.Map<String, Object>) report).put("status", "RESOLVED");
        }
        return ApiResponse.ok("RESOLVED_" + id);
    }

    @PostMapping("/resources/{id}/block")
    public ApiResponse<String> blockResource(@PathVariable Long id) {
        return ApiResponse.ok("BLOCKED_RESOURCE_" + id);
    }

    @PostMapping("/users/{id}/ban")
    public ApiResponse<String> banUser(@PathVariable Long id) {
        return ApiResponse.ok("BANNED_USER_" + id);
    }
}
