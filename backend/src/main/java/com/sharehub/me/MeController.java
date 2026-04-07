package com.sharehub.me;

import com.sharehub.common.ApiResponse;
import com.sharehub.common.PageResponse;
import com.sharehub.note.NoteDto;
import com.sharehub.resource.ResourceDto;
import com.sharehub.resume.ResumeDto;
import com.sharehub.roadmap.RoadmapDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final MeService meService;

    public MeController(MeService meService) {
        this.meService = meService;
    }

    @GetMapping
    public ApiResponse<MeDto> getMe() {
        return ApiResponse.ok(meService.aggregate("local-dev-user"));
    }

    @GetMapping("/resources")
    public ApiResponse<PageResponse<ResourceDto>> myResources(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ApiResponse.ok(meService.myResources(page, pageSize));
    }

    @GetMapping("/roadmaps")
    public ApiResponse<PageResponse<RoadmapDto>> myRoadmaps(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ApiResponse.ok(meService.myRoadmaps(page, pageSize));
    }

    @GetMapping("/favorites")
    public ApiResponse<PageResponse<ResourceDto>> myFavorites(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ApiResponse.ok(meService.myFavorites(page, pageSize));
    }

    @GetMapping("/notes")
    public ApiResponse<PageResponse<NoteDto>> myNotes(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ApiResponse.ok(meService.myNotes(page, pageSize));
    }

    @GetMapping("/resumes")
    public ApiResponse<PageResponse<ResumeDto>> myResumes(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ApiResponse.ok(meService.myResumes(page, pageSize));
    }
}
