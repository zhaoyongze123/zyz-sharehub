package com.sharehub.resume;

import com.sharehub.common.ApiResponse;
import com.sharehub.common.InMemoryStore;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final InMemoryStore store;

    public ResumeController(InMemoryStore store) {
        this.store = store;
    }

    @PostMapping("/generate")
    public ApiResponse<Map<String, Object>> generate(@RequestBody Map<String, Object> req) {
        long id = store.nextId();
        Map<String, Object> saved = new HashMap<>();
        saved.put("id", id);
        saved.put("templateKey", req.getOrDefault("templateKey", "default"));
        saved.put("status", "GENERATED");
        saved.put("fileUrl", "/api/resumes/" + id + "/download");
        store.resumes.put(id, saved);
        return ApiResponse.ok(saved);
    }

    @GetMapping("/{id}")
    public ApiResponse<Object> detail(@PathVariable Long id) {
        return ApiResponse.ok(store.resumes.get(id));
    }

    @GetMapping("/{id}/download")
    public ApiResponse<String> download(@PathVariable Long id) {
        return ApiResponse.ok("PDF_DOWNLOAD_PLACEHOLDER_" + id);
    }
}
