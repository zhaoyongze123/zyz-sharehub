package com.sharehub.interaction;

import com.sharehub.common.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class InteractionController {

    private final InteractionRepository repository;

    public InteractionController(InteractionRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/resources/{id}/comments")
    public ApiResponse<InteractionRepository.CommentRecord> comment(@PathVariable Long id, @RequestBody Map<String, String> req) {
        InteractionRepository.CommentRecord comment = repository.saveComment(id, req.get("content"), null);
        return ApiResponse.ok(comment);
    }

    @GetMapping("/resources/{id}/comments")
    public ApiResponse<java.util.List<CommentNodeDto>> comments(@PathVariable Long id) {
        return ApiResponse.ok(repository.listCommentTreeByResource(id));
    }

    @PostMapping("/comments/{id}/reply")
    public ApiResponse<InteractionRepository.CommentRecord> reply(@PathVariable Long id, @RequestBody Map<String, String> req) {
        InteractionRepository.CommentRecord reply = repository.saveComment(id, req.get("content"), id);
        return ApiResponse.ok(reply);
    }

    @PostMapping("/resources/{id}/favorite")
    public ApiResponse<Map<String, Object>> favorite(@PathVariable Long id) {
        int total = repository.addFavorite(id);
        return ApiResponse.ok(Map.of("resourceId", id, "favorites", total));
    }

    @PostMapping("/resources/{id}/like")
    public ApiResponse<Map<String, Object>> like(@PathVariable Long id) {
        int total = repository.addLike(id);
        return ApiResponse.ok(Map.of("resourceId", id, "likes", total));
    }

    @GetMapping("/resources/{id}/interactions")
    public ApiResponse<InteractionSummaryDto> interactions(@PathVariable Long id) {
        return ApiResponse.ok(repository.summarizeResource(id));
    }

    @PostMapping("/reports")
    public ApiResponse<InteractionRepository.ReportRecord> report(@RequestBody Map<String, String> req) {
        long resourceId = Long.parseLong(req.getOrDefault("resourceId", "0"));
        String reason = req.getOrDefault("reason", "无");
        String reporter = req.getOrDefault("reporter", "anonymous");
        InteractionRepository.ReportRecord report = repository.saveReport(resourceId, reason, reporter);
        return ApiResponse.ok(report);
    }
}
