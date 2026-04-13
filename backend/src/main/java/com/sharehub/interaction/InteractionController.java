package com.sharehub.interaction;

import com.sharehub.auth.RequestAccessService;
import com.sharehub.auth.UserProfileRepository;
import com.sharehub.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class InteractionController {

    private final InteractionRepository repository;
    private final RequestAccessService requestAccessService;
    private final UserProfileRepository userProfileRepository;

    public InteractionController(
        InteractionRepository repository,
        RequestAccessService requestAccessService,
        UserProfileRepository userProfileRepository
    ) {
        this.repository = repository;
        this.requestAccessService = requestAccessService;
        this.userProfileRepository = userProfileRepository;
    }

    @PostMapping("/resources/{id}/comments")
    public ApiResponse<InteractionRepository.CommentRecord> comment(
        Authentication authentication,
        HttpServletRequest request,
        @PathVariable Long id,
        @RequestBody Map<String, String> req
    ) {
        String author = requireActiveUser(authentication, request);
        InteractionRepository.CommentRecord comment = repository.saveComment(id, req.get("content"), null, author);
        return ApiResponse.ok(comment);
    }

    @GetMapping("/resources/{id}/comments")
    public ApiResponse<java.util.List<CommentNodeDto>> comments(@PathVariable Long id) {
        return ApiResponse.ok(repository.listCommentTreeByResource(id));
    }

    @PostMapping("/comments/{id}/reply")
    public ApiResponse<InteractionRepository.CommentRecord> reply(
        Authentication authentication,
        HttpServletRequest request,
        @PathVariable Long id,
        @RequestBody Map<String, String> req
    ) {
        String author = requireActiveUser(authentication, request);
        InteractionRepository.CommentRecord reply = repository.saveComment(id, req.get("content"), id, author);
        return ApiResponse.ok(reply);
    }

    @PostMapping("/resources/{id}/favorite")
    public ApiResponse<Map<String, Object>> favorite(
        Authentication authentication,
        HttpServletRequest request,
        @PathVariable Long id
    ) {
        String userKey = requireActiveUser(authentication, request);
        int total = repository.addFavorite(id, userKey);
        return ApiResponse.ok(Map.of("resourceId", id, "favorites", total));
    }

    @DeleteMapping("/resources/{id}/favorite")
    public ApiResponse<Map<String, Object>> unfavorite(
        Authentication authentication,
        HttpServletRequest request,
        @PathVariable Long id
    ) {
        String userKey = requireActiveUser(authentication, request);
        int total = repository.removeFavorite(id, userKey);
        return ApiResponse.ok(Map.of("resourceId", id, "favorites", total));
    }

    @PostMapping("/resources/{id}/like")
    public ApiResponse<Map<String, Object>> like(
        Authentication authentication,
        HttpServletRequest request,
        @PathVariable Long id
    ) {
        String userKey = requireActiveUser(authentication, request);
        int total = repository.addLike(id, userKey);
        return ApiResponse.ok(Map.of("resourceId", id, "likes", total));
    }

    @DeleteMapping("/resources/{id}/like")
    public ApiResponse<Map<String, Object>> unlike(
        Authentication authentication,
        HttpServletRequest request,
        @PathVariable Long id
    ) {
        String userKey = requireActiveUser(authentication, request);
        int total = repository.removeLike(id, userKey);
        return ApiResponse.ok(Map.of("resourceId", id, "likes", total));
    }

    @PostMapping("/notes/{id}/favorite")
    public ApiResponse<Map<String, Object>> favoriteNote(
        Authentication authentication,
        HttpServletRequest request,
        @PathVariable Long id
    ) {
        String userKey = requireActiveUser(authentication, request);
        int total = repository.addNoteFavorite(id, userKey);
        return ApiResponse.ok(Map.of("noteId", id, "favorites", total));
    }

    @DeleteMapping("/notes/{id}/favorite")
    public ApiResponse<Map<String, Object>> unfavoriteNote(
        Authentication authentication,
        HttpServletRequest request,
        @PathVariable Long id
    ) {
        String userKey = requireActiveUser(authentication, request);
        int total = repository.removeNoteFavorite(id, userKey);
        return ApiResponse.ok(Map.of("noteId", id, "favorites", total));
    }

    @PostMapping("/notes/{id}/like")
    public ApiResponse<Map<String, Object>> likeNote(
        Authentication authentication,
        HttpServletRequest request,
        @PathVariable Long id
    ) {
        String userKey = requireActiveUser(authentication, request);
        int total = repository.addNoteLike(id, userKey);
        return ApiResponse.ok(Map.of("noteId", id, "likes", total));
    }

    @DeleteMapping("/notes/{id}/like")
    public ApiResponse<Map<String, Object>> unlikeNote(
        Authentication authentication,
        HttpServletRequest request,
        @PathVariable Long id
    ) {
        String userKey = requireActiveUser(authentication, request);
        int total = repository.removeNoteLike(id, userKey);
        return ApiResponse.ok(Map.of("noteId", id, "likes", total));
    }

    @GetMapping("/resources/{id}/interactions")
    public ApiResponse<InteractionSummaryDto> interactions(@PathVariable Long id) {
        return ApiResponse.ok(repository.summarizeResource(id));
    }

    @GetMapping("/notes/{id}/interactions")
    public ApiResponse<NoteInteractionSummaryDto> noteInteractions(
        Authentication authentication,
        HttpServletRequest request,
        @PathVariable Long id
    ) {
        String viewerKey = requestAccessService.resolveUser(authentication, request)
            .map((login) -> {
                userProfileRepository.upsert(login, login, null);
                userProfileRepository.ensureActive(login);
                return login;
            })
            .orElse(null);
        return ApiResponse.ok(repository.summarizeNote(id, viewerKey));
    }

    @PostMapping("/reports")
    public ApiResponse<InteractionRepository.ReportRecord> report(
        Authentication authentication,
        HttpServletRequest request,
        @RequestBody Map<String, String> req
    ) {
        String reporter = requireActiveUser(authentication, request);
        String targetType = req.getOrDefault("targetType", req.containsKey("noteId") ? "NOTE" : "RESOURCE")
            .trim()
            .toUpperCase(Locale.ROOT);
        String reason = req.getOrDefault("reason", "无");
        InteractionRepository.ReportRecord report = switch (targetType) {
            case "NOTE" -> repository.saveNoteReport(Long.parseLong(req.getOrDefault("noteId", "0")), reason, reporter);
            case "RESOURCE" -> repository.saveReport(Long.parseLong(req.getOrDefault("resourceId", "0")), reason, reporter);
            default -> throw new IllegalArgumentException("Unsupported target type: " + targetType);
        };
        return ApiResponse.ok(report);
    }

    private String requireActiveUser(Authentication authentication, HttpServletRequest request) {
        String userKey = requestAccessService.requireUser(authentication, request);
        userProfileRepository.upsert(userKey, userKey, null);
        userProfileRepository.ensureActive(userKey);
        return userKey;
    }
}
