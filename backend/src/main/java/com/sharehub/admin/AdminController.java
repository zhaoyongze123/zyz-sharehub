package com.sharehub.admin;

import com.sharehub.common.ApiResponse;
import com.sharehub.interaction.InteractionRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final InteractionRepository interactionRepository;

    public AdminController(InteractionRepository interactionRepository) {
        this.interactionRepository = interactionRepository;
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
    public ApiResponse<String> banUser(@PathVariable Long id) {
        return ApiResponse.ok("BANNED_USER_" + id);
    }
}
