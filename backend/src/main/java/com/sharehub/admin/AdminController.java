package com.sharehub.admin;

import com.sharehub.auth.UserProfileDto;
import com.sharehub.auth.UserProfileRepository;
import com.sharehub.common.ApiResponse;
import com.sharehub.common.PageResponse;
import com.sharehub.interaction.InteractionRepository;
import com.sharehub.resource.ResourceDto;
import com.sharehub.resource.ResourceEntity;
import com.sharehub.resource.ResourceRepository;
import jakarta.transaction.Transactional;
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
    public ApiResponse<PageResponse<InteractionRepository.ReportRecord>> reports(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int pageSize,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String targetType
    ) {
        return ApiResponse.ok(interactionRepository.findReports(page, pageSize, status, targetType));
    }

    @GetMapping("/audit-logs")
    public ApiResponse<PageResponse<AdminAuditLogDto>> auditLogs(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int pageSize,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) String targetType
    ) {
        return ApiResponse.ok(interactionRepository.listAuditLogs(page, pageSize, action, targetType));
    }

    @PostMapping("/reports/{id}/resolve")
    @Transactional
    public ApiResponse<InteractionRepository.ReportRecord> resolve(@PathVariable Long id) {
        InteractionRepository.ReportRecord resolved = interactionRepository.resolveReport(id);
        if ("RESOURCE".equalsIgnoreCase(resolved.targetType())) {
            resourceRepository.findById(resolved.targetId()).ifPresent(resource -> {
                resource.setStatus("REMOVED");
                resourceRepository.save(resource);
            });
            interactionRepository.appendAuditLog(
                "AUTO_REMOVE_RESOURCE",
                "RESOURCE",
                String.valueOf(resolved.targetId()),
                "{\"fromReportId\":\"" + id + "\"}"
            );
        }
        return ApiResponse.ok(resolved);
    }

    @PostMapping("/resources/{id}/block")
    @Transactional
    public ApiResponse<ResourceDto> blockResource(@PathVariable Long id) {
        ResourceEntity resource = resourceRepository.findById(id)
            .orElseThrow(() -> new com.sharehub.common.NotFoundException("RESOURCE_NOT_FOUND"));
        resource.setStatus("REMOVED");
        interactionRepository.appendAuditLog("BLOCK_RESOURCE", "RESOURCE", String.valueOf(id), "{}");
        return ApiResponse.ok(resourceRepository.save(resource).toDto());
    }

    @PostMapping("/resources/{id}/restore")
    @Transactional
    public ApiResponse<ResourceDto> restoreResource(@PathVariable Long id) {
        ResourceEntity resource = resourceRepository.findById(id)
            .orElseThrow(() -> new com.sharehub.common.NotFoundException("RESOURCE_NOT_FOUND"));
        resource.setStatus("PUBLISHED");
        interactionRepository.appendAuditLog("RESTORE_RESOURCE", "RESOURCE", String.valueOf(id), "{}");
        return ApiResponse.ok(resourceRepository.save(resource).toDto());
    }

    @PostMapping("/users/{id}/ban")
    public ApiResponse<UserProfileDto> banUser(@PathVariable Long id) {
        UserProfileDto profile = userProfileRepository.updateStatus(id, "BANNED");
        interactionRepository.appendAuditLog("BAN_USER", "USER", String.valueOf(id), "{}");
        return ApiResponse.ok(profile);
    }

    @PostMapping("/users/{id}/unban")
    public ApiResponse<UserProfileDto> unbanUser(@PathVariable Long id) {
        UserProfileDto profile = userProfileRepository.updateStatus(id, "ACTIVE");
        interactionRepository.appendAuditLog("UNBAN_USER", "USER", String.valueOf(id), "{}");
        return ApiResponse.ok(profile);
    }

    @PostMapping("/comments/{id}/hide")
    @Transactional
    public ApiResponse<InteractionRepository.CommentRecord> hideComment(@PathVariable Long id) {
        InteractionRepository.CommentRecord record = interactionRepository.updateCommentStatus(id, "HIDDEN");
        interactionRepository.appendAuditLog("HIDE_COMMENT", "COMMENT", String.valueOf(id), "{}");
        return ApiResponse.ok(record);
    }

    @PostMapping("/comments/{id}/restore")
    @Transactional
    public ApiResponse<InteractionRepository.CommentRecord> restoreComment(@PathVariable Long id) {
        InteractionRepository.CommentRecord record = interactionRepository.updateCommentStatus(id, "VISIBLE");
        interactionRepository.appendAuditLog("RESTORE_COMMENT", "COMMENT", String.valueOf(id), "{}");
        return ApiResponse.ok(record);
    }
}
