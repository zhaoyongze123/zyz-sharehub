package com.sharehub.roadmap;

import com.sharehub.auth.RequestAccessService;
import com.sharehub.auth.UserProfileRepository;
import com.sharehub.common.ApiResponse;
import com.sharehub.common.NotFoundException;
import com.sharehub.common.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
        RoadmapDetailResponse detail = service.detail(id, resolveActiveUser(authentication, request));
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

    @PostMapping(path = "/{id}/nodes/{nodeId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<RoadmapNodeAttachmentDto> uploadNodeAttachment(
        Authentication authentication,
        HttpServletRequest request,
        @PathVariable Long id,
        @PathVariable Long nodeId,
        @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.ok(service.uploadNodeAttachment(requireActiveUser(authentication, request), id, nodeId, file));
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

    @PostMapping("/{id}/enrollment")
    public ApiResponse<RoadmapEnrollmentDto> enroll(
        Authentication authentication,
        HttpServletRequest request,
        @PathVariable Long id
    ) {
        return ApiResponse.ok(service.enroll(requireActiveUser(authentication, request), id));
    }

    @GetMapping("/{id}/enrollment")
    public ApiResponse<RoadmapEnrollmentDto> enrollment(
        Authentication authentication,
        HttpServletRequest request,
        @PathVariable Long id
    ) {
        return ApiResponse.ok(service.getEnrollment(requireActiveUser(authentication, request), id));
    }

    @PostMapping("/{id}/enrollment/pause")
    public ApiResponse<RoadmapEnrollmentDto> pauseEnrollment(
        Authentication authentication,
        HttpServletRequest request,
        @PathVariable Long id
    ) {
        return ApiResponse.ok(service.pauseEnrollment(requireActiveUser(authentication, request), id));
    }

    @PostMapping("/{id}/enrollment/resume")
    public ApiResponse<RoadmapEnrollmentDto> resumeEnrollment(
        Authentication authentication,
        HttpServletRequest request,
        @PathVariable Long id
    ) {
        return ApiResponse.ok(service.resumeEnrollment(requireActiveUser(authentication, request), id));
    }

    @PostMapping("/{id}/enrollment/complete")
    public ApiResponse<RoadmapEnrollmentDto> completeEnrollment(
        Authentication authentication,
        HttpServletRequest request,
        @PathVariable Long id
    ) {
        return ApiResponse.ok(service.completeEnrollment(requireActiveUser(authentication, request), id));
    }

    private String resolveActiveUser(Authentication authentication, HttpServletRequest request) {
        Optional<String> resolvedUser = requestAccessService.resolveUser(authentication, request);
        if (resolvedUser.isEmpty()) {
            return null;
        }
        String userKey = resolvedUser.get();
        userProfileRepository.upsert(userKey, userKey, null);
        userProfileRepository.ensureActive(userKey);
        return userKey;
    }

    private String requireActiveUser(Authentication authentication, HttpServletRequest request) {
        String ownerKey = requestAccessService.requireUser(authentication, request);
        userProfileRepository.upsert(ownerKey, ownerKey, null);
        userProfileRepository.ensureActive(ownerKey);
        return ownerKey;
    }
}
