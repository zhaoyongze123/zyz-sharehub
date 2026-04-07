package com.sharehub.admin;

import com.sharehub.auth.UserProfileDto;
import com.sharehub.auth.UserProfileRepository;
import com.sharehub.common.ApiResponse;
import com.sharehub.interaction.InteractionRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final InteractionRepository interactionRepository;
    private final UserProfileRepository userProfileRepository;

    public AdminController(InteractionRepository interactionRepository, UserProfileRepository userProfileRepository) {
        this.interactionRepository = interactionRepository;
        this.userProfileRepository = userProfileRepository;
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
    public ApiResponse<String> blockResource(@PathVariable Long id) {
        return ApiResponse.ok("BLOCKED_RESOURCE_" + id);
    }

    @PostMapping("/users/{id}/ban")
    public ApiResponse<UserProfileDto> banUser(@PathVariable Long id) {
        return ApiResponse.ok(userProfileRepository.updateStatus(id, "BANNED"));
    }

    @PostMapping("/users/{id}/unban")
    public ApiResponse<UserProfileDto> unbanUser(@PathVariable Long id) {
        return ApiResponse.ok(userProfileRepository.updateStatus(id, "ACTIVE"));
    }
}
