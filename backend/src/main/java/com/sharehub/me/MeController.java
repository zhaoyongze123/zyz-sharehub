package com.sharehub.me;

import com.sharehub.auth.RequestAccessService;
import com.sharehub.auth.UserProfileRepository;
import com.sharehub.common.ApiResponse;
import com.sharehub.common.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import com.sharehub.note.NoteDto;
import com.sharehub.resource.ResourceDto;
import com.sharehub.resume.ResumeDto;
import com.sharehub.roadmap.RoadmapWorkbenchDto;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final MeService meService;
    private final RequestAccessService requestAccessService;
    private final UserProfileRepository userProfileRepository;

    public MeController(
        MeService meService,
        RequestAccessService requestAccessService,
        UserProfileRepository userProfileRepository
    ) {
        this.meService = meService;
        this.requestAccessService = requestAccessService;
        this.userProfileRepository = userProfileRepository;
    }

    @GetMapping
    public ApiResponse<MeDto> getMe(Authentication authentication, HttpServletRequest request) {
        String ownerKey = requireActiveUser(authentication, request);
        return ApiResponse.ok(meService.aggregate(ownerKey));
    }

    @GetMapping("/resources")
    public ApiResponse<PageResponse<ResourceDto>> myResources(
        Authentication authentication,
        HttpServletRequest request,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String visibility
    ) {
        String ownerKey = requireActiveUser(authentication, request);
        return ApiResponse.ok(meService.myResources(ownerKey, status, visibility, page, pageSize));
    }

    @GetMapping("/roadmaps")
    public ApiResponse<PageResponse<RoadmapWorkbenchDto>> myRoadmaps(
        Authentication authentication,
        HttpServletRequest request,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) String status
    ) {
        String ownerKey = requireActiveUser(authentication, request);
        return ApiResponse.ok(meService.myRoadmaps(ownerKey, status, page, pageSize));
    }

    @GetMapping("/favorites")
    public ApiResponse<PageResponse<ResourceDto>> myFavorites(
        Authentication authentication,
        HttpServletRequest request,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int pageSize
    ) {
        String ownerKey = requireActiveUser(authentication, request);
        return ApiResponse.ok(meService.myFavorites(ownerKey, page, pageSize));
    }

    @GetMapping("/notes")
    public ApiResponse<PageResponse<NoteDto>> myNotes(
        Authentication authentication,
        HttpServletRequest request,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) String status
    ) {
        String ownerKey = requireActiveUser(authentication, request);
        return ApiResponse.ok(meService.myNotes(ownerKey, status, page, pageSize));
    }

    @GetMapping("/resumes")
    public ApiResponse<PageResponse<ResumeDto>> myResumes(
        Authentication authentication,
        HttpServletRequest request,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String templateKey,
        @RequestParam(required = false) String keyword
    ) {
        String ownerKey = requireActiveUser(authentication, request);
        return ApiResponse.ok(meService.myResumes(ownerKey, status, templateKey, keyword, page, pageSize));
    }

    private String requireActiveUser(Authentication authentication, HttpServletRequest request) {
        String ownerKey = requestAccessService.requireUser(authentication, request);
        userProfileRepository.upsert(ownerKey, ownerKey, null);
        userProfileRepository.ensureActive(ownerKey);
        return ownerKey;
    }
}
