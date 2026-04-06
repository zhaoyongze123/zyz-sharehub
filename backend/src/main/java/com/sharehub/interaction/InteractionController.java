package com.sharehub.interaction;

import com.sharehub.common.ApiResponse;
import com.sharehub.common.InMemoryStore;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class InteractionController {

    private final InMemoryStore store;

    public InteractionController(InMemoryStore store) {
        this.store = store;
    }

    @PostMapping("/resources/{id}/comments")
    public ApiResponse<Map<String, Object>> comment(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        Map<String, Object> result = new HashMap<>();
        result.put("resourceId", id);
        result.put("commentId", store.nextId());
        result.put("content", req.get("content"));
        return ApiResponse.ok(result);
    }

    @PostMapping("/comments/{id}/reply")
    public ApiResponse<Map<String, Object>> reply(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        Map<String, Object> result = new HashMap<>();
        result.put("parentCommentId", id);
        result.put("commentId", store.nextId());
        result.put("content", req.get("content"));
        return ApiResponse.ok(result);
    }

    @PostMapping("/resources/{id}/favorite")
    public ApiResponse<String> favorite(@PathVariable Long id) {
        return ApiResponse.ok("FAVORITED_" + id);
    }

    @PostMapping("/resources/{id}/like")
    public ApiResponse<String> like(@PathVariable Long id) {
        return ApiResponse.ok("LIKED_" + id);
    }

    @PostMapping("/reports")
    public ApiResponse<Map<String, Object>> report(@RequestBody Map<String, Object> req) {
        long id = store.nextId();
        req.put("id", id);
        req.put("status", "OPEN");
        store.reports.put(id, req);
        return ApiResponse.ok(req);
    }
}
