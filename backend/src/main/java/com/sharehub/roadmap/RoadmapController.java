package com.sharehub.roadmap;

import com.sharehub.common.ApiResponse;
import com.sharehub.common.InMemoryStore;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/roadmaps")
public class RoadmapController {

    private final InMemoryStore store;
    private final Map<Long, List<RoadmapNodeDto>> nodes = new HashMap<>();
    private final Map<Long, Map<String, Object>> progress = new HashMap<>();

    public RoadmapController(InMemoryStore store) {
        this.store = store;
    }

    @PostMapping
    public ApiResponse<RoadmapDto> create(@Valid @RequestBody RoadmapDto req) {
        long id = store.nextId();
        RoadmapDto saved = new RoadmapDto(id, req.title(), req.description(), req.visibility(), "PUBLISHED");
        store.roadmaps.put(id, saved);
        return ApiResponse.ok(saved);
    }

    @GetMapping
    public ApiResponse<List<Object>> list() {
        return ApiResponse.ok(new ArrayList<>(store.roadmaps.values()));
    }

    @GetMapping("/{id}")
    public ApiResponse<Object> detail(@PathVariable Long id) {
        return ApiResponse.ok(store.roadmaps.get(id));
    }

    @PostMapping("/{id}/nodes")
    public ApiResponse<List<RoadmapNodeDto>> addNode(@PathVariable Long id, @Valid @RequestBody RoadmapNodeDto req) {
        RoadmapNodeDto node = new RoadmapNodeDto(store.nextId(), req.parentId(), req.title(), req.orderNo(), req.resourceId(), req.noteId());
        nodes.computeIfAbsent(id, k -> new ArrayList<>()).add(node);
        return ApiResponse.ok(nodes.get(id));
    }

    @PostMapping("/{id}/progress")
    public ApiResponse<Map<String, Object>> progress(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        progress.put(id, req);
        return ApiResponse.ok(progress.get(id));
    }
}
