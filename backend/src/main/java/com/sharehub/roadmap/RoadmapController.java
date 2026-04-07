package com.sharehub.roadmap;

import com.sharehub.common.ApiResponse;
import com.sharehub.common.NotFoundException;
import com.sharehub.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/roadmaps")
public class RoadmapController {

    private final RoadmapService service;

    public RoadmapController(RoadmapService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<RoadmapDto> create(@Valid @RequestBody RoadmapDto req) {
        return ApiResponse.ok(service.create(req));
    }

    @GetMapping
    public ApiResponse<PageResponse<RoadmapDto>> list(
        @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(service.list(page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<RoadmapDetailResponse> detail(@PathVariable Long id) {
        RoadmapDetailResponse detail = service.detail(id);
        if (detail == null) {
            throw new NotFoundException("ROADMAP_NOT_FOUND");
        }
        return ApiResponse.ok(detail);
    }

    @PostMapping("/{id}/nodes")
    public ApiResponse<List<RoadmapNodeDto>> addNode(@PathVariable Long id, @Valid @RequestBody RoadmapNodeDto req) {
        return ApiResponse.ok(service.addNode(id, req));
    }

    @PostMapping("/{id}/progress")
    public ApiResponse<Map<String, Object>> progress(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        return ApiResponse.ok(service.updateProgress(id, req));
    }
}
