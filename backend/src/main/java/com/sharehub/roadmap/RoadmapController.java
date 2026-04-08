package com.sharehub.roadmap;

import com.sharehub.auth.RequestAccessService;
import com.sharehub.auth.UserProfileRepository;
import com.sharehub.common.ApiResponse;
import com.sharehub.common.NotFoundException;
import com.sharehub.common.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/roadmaps")
public class RoadmapController {

    private final RoadmapService service;
    private final RequestAccessService requestAccessService;
    private final UserProfileRepository userProfileRepository;

    public RoadmapController(
        RoadmapService service,
        RequestAccessService requestAccessService,
        UserProfileRepository userProfileRepository
    ) {
        this.service = service;
        this.requestAccessService = requestAccessService;
        this.userProfileRepository = userProfileRepository;
    }

    @PostMapping
    public ApiResponse<RoadmapDto> create(
        Authentication authentication,
        HttpServletRequest request,
        @Valid @RequestBody RoadmapDto req
    ) {
        return ApiResponse.ok(service.create(requireActiveUser(authentication, request), req));
    }

    @GetMapping
    public ApiResponse<PageResponse<RoadmapDto>> list(
        @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(service.list(page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<RoadmapDetailResponse> detail(
        Authentication authentication,
        HttpServletRequest request,
        @PathVariable Long id
    ) {
        RoadmapDetailResponse detail = service.detail(id, resolveUser(authentication, request));
        if (detail == null) {
            throw new NotFoundException("ROADMAP_NOT_FOUND");
        }
        return ApiResponse.ok(detail);
    }

    @PostMapping("/{id}/nodes")
    public ApiResponse<List<RoadmapNodeDto>> addNode(
        Authentication authentication,
        HttpServletRequest request,
        @PathVariable Long id,
        @Valid @RequestBody RoadmapNodeDto req
    ) {
        return ApiResponse.ok(service.addNode(requireActiveUser(authentication, request), id, req));
    }

    @PostMapping("/{id}/progress")
    public ApiResponse<Map<String, Object>> progress(
        Authentication authentication,
        HttpServletRequest request,
        @PathVariable Long id,
        @RequestBody Map<String, Object> req
    ) {
        return ApiResponse.ok(service.updateProgress(requireActiveUser(authentication, request), id, req));
    }

    private String resolveUser(Authentication authentication, HttpServletRequest request) {
        return requestAccessService.resolveUser(authentication, request).orElse(null);
    }

    private String requireActiveUser(Authentication authentication, HttpServletRequest request) {
        String ownerKey = requestAccessService.requireUser(authentication, request);
        userProfileRepository.upsert(ownerKey, ownerKey, null);
        userProfileRepository.ensureActive(ownerKey);
        return ownerKey;
    }
}
