package com.sharehub.admin;

import com.sharehub.auth.UserProfileDto;
import com.sharehub.auth.UserProfileRepository;
import com.sharehub.common.ApiResponse;
import com.sharehub.interaction.InteractionRepository;
import com.sharehub.resource.ResourceDto;
import com.sharehub.resource.ResourceEntity;
import com.sharehub.resource.ResourceRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final InteractionRepository interactionRepository;
    private final UserProfileRepository userProfileRepository;
    private final ResourceRepository resourceRepository;

    public AdminController(
        InteractionRepository interactionRepository,
        UserProfileRepository userProfileRepository,
        ResourceRepository resourceRepository
    ) {
        this.interactionRepository = interactionRepository;
        this.userProfileRepository = userProfileRepository;
        this.resourceRepository = resourceRepository;
    }

    @GetMapping("/reports")
    public ApiResponse<List<InteractionRepository.ReportRecord>> reports() {
        return ApiResponse.ok(interactionRepository.listReports());
    }

    @PostMapping("/reports/{id}/resolve")
    public ApiResponse<InteractionRepository.ReportRecord> resolve(@PathVariable Long id) {
        InteractionRepository.ReportRecord resolved = interactionRepository.resolveReport(id);
        return ApiResponse.ok(resolved);
    }

    @PostMapping("/resources/{id}/block")
    public ApiResponse<ResourceDto> blockResource(@PathVariable Long id) {
        ResourceEntity resource = resourceRepository.findById(id)
            .orElseThrow(() -> new com.sharehub.common.NotFoundException("RESOURCE_NOT_FOUND"));
        resource.setStatus("REMOVED");
        return ApiResponse.ok(resourceRepository.save(resource).toDto());
    }

    @PostMapping("/resources/{id}/restore")
    public ApiResponse<ResourceDto> restoreResource(@PathVariable Long id) {
        ResourceEntity resource = resourceRepository.findById(id)
            .orElseThrow(() -> new com.sharehub.common.NotFoundException("RESOURCE_NOT_FOUND"));
        resource.setStatus("PUBLISHED");
        return ApiResponse.ok(resourceRepository.save(resource).toDto());
    }

    @PostMapping("/users/{id}/ban")
    public ApiResponse<UserProfileDto> banUser(@PathVariable Long id) {
        return ApiResponse.ok(userProfileRepository.updateStatus(id, "BANNED"));
    }

    @PostMapping("/users/{id}/unban")
    public ApiResponse<UserProfileDto> unbanUser(@PathVariable Long id) {
        return ApiResponse.ok(userProfileRepository.updateStatus(id, "ACTIVE"));
    }

    @PostMapping("/comments/{id}/hide")
    public ApiResponse<InteractionRepository.CommentRecord> hideComment(@PathVariable Long id) {
        return ApiResponse.ok(interactionRepository.updateCommentStatus(id, "HIDDEN"));
    }

    @PostMapping("/comments/{id}/restore")
    public ApiResponse<InteractionRepository.CommentRecord> restoreComment(@PathVariable Long id) {
        return ApiResponse.ok(interactionRepository.updateCommentStatus(id, "VISIBLE"));
    }
}
