package com.sharehub.note;

import com.sharehub.auth.RequestAccessService;
import com.sharehub.auth.UserProfileRepository;
import com.sharehub.auth.AdminWhitelistRepository;
import com.sharehub.common.ApiResponse;
import com.sharehub.common.PageResponse;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import com.sharehub.config.AdminTokenFilter;
import com.sharehub.tag.TagAssignmentRepository;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteRepository repository;
    private final RequestAccessService requestAccessService;
    private final UserProfileRepository userProfileRepository;
    private final AdminWhitelistRepository adminWhitelistRepository;
    private final String expectedAdminToken;
    private final TagAssignmentRepository tagAssignmentRepository;

    public NoteController(
        NoteRepository repository,
        RequestAccessService requestAccessService,
        UserProfileRepository userProfileRepository,
        AdminWhitelistRepository adminWhitelistRepository,
        Environment environment,
        TagAssignmentRepository tagAssignmentRepository
    ) {
        this.repository = repository;
        this.requestAccessService = requestAccessService;
        this.userProfileRepository = userProfileRepository;
        this.adminWhitelistRepository = adminWhitelistRepository;
        this.expectedAdminToken = environment.getProperty("sharehub.admin.token", AdminTokenFilter.DEFAULT_ADMIN_TOKEN);
        this.tagAssignmentRepository = tagAssignmentRepository;
    }

    @PostMapping
    public ApiResponse<NoteDto> create(
        Authentication authentication,
        HttpServletRequest request,
        @Valid @RequestBody NoteDto req
    ) {
        String ownerKey = requireActiveUser(authentication, request);
        boolean isAdmin = hasAdminPermission(authentication, request, ownerKey);
        NoteDto toSave = new NoteDto(
            null,
            req.title(),
            req.contentMd(),
            req.visibility(),
            req.status(),
            req.category(),
            tagAssignmentRepository.normalizeTags(req.tags()),
            null,
            null,
            null,
            null,
            null,
            isAdmin,
            isAdmin && req.isPinned()
        );
        NoteDto saved = repository.save(ownerKey, toSave);
        return ApiResponse.ok(saved);
    }

    @GetMapping
    public ApiResponse<PageResponse<NoteDto>> list(
        Authentication authentication,
        HttpServletRequest request,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) String status) {
        String ownerKey = requireActiveUser(authentication, request);
        return ApiResponse.ok(repository.listByOwner(ownerKey, status, page, pageSize));
    }

    @GetMapping("/community")
    public ApiResponse<PageResponse<NoteDto>> community(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ApiResponse.ok(repository.listPublished(page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<NoteDto> detail(
        Authentication authentication,
        HttpServletRequest request,
        @PathVariable Long id
    ) {
        String ownerKey = requestAccessService.resolveUser(authentication, request)
            .map((login) -> {
                userProfileRepository.upsert(login, login, null);
                userProfileRepository.ensureActive(login);
                return login;
            })
            .orElse(null);
        NoteDto note = repository.findAccessible(id, ownerKey);
        if (ownerKey != null) {
            repository.recordView(id, ownerKey);
        }
        return ApiResponse.ok(note);
    }

    @GetMapping("/{id}/related")
    public ApiResponse<List<RelatedNoteDto>> related(
        Authentication authentication,
        HttpServletRequest request,
        @PathVariable Long id
    ) {
        String ownerKey = requestAccessService.resolveUser(authentication, request)
            .map((login) -> {
                userProfileRepository.upsert(login, login, null);
                userProfileRepository.ensureActive(login);
                return login;
            })
            .orElse(null);
        return ApiResponse.ok(repository.findRelated(id, ownerKey));
    }

    @PutMapping("/{id}")
    public ApiResponse<NoteDto> update(
        Authentication authentication,
        HttpServletRequest request,
        @PathVariable Long id,
        @Valid @RequestBody NoteDto req
    ) {
        String ownerKey = requireActiveUser(authentication, request);
        NoteDto updated = repository.upsertOwned(id, ownerKey, sanitizeForUpdate(authentication, request, ownerKey, req));
        return ApiResponse.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(
        Authentication authentication,
        HttpServletRequest request,
        @PathVariable Long id
    ) {
        String ownerKey = requireActiveUser(authentication, request);
        if (hasAdminPermission(authentication, request, ownerKey)) {
            repository.deleteById(id);
        } else {
            repository.deleteOwned(id, ownerKey);
        }
        return ApiResponse.ok("DELETED");
    }

    private String requireActiveUser(Authentication authentication, HttpServletRequest request) {
        String login = requestAccessService.requireUser(authentication, request);
        userProfileRepository.upsert(login, login, null);
        userProfileRepository.ensureActive(login);
        return login;
    }

    private NoteDto sanitizeForUpdate(
        Authentication authentication,
        HttpServletRequest request,
        String ownerKey,
        NoteDto req
    ) {
        boolean isAdmin = hasAdminPermission(authentication, request, ownerKey);
        return new NoteDto(
            req.id(),
            req.title(),
            req.contentMd(),
            req.visibility(),
            req.status(),
            req.category(),
            tagAssignmentRepository.normalizeTags(req.tags()),
            req.ownerKey(),
            req.ownerName(),
            req.ownerAvatarUrl(),
            req.createdAt(),
            req.updatedAt(),
            isAdmin,
            isAdmin && req.isPinned()
        );
    }

    private boolean hasAdminPermission(Authentication authentication, HttpServletRequest request, String login) {
        if (authentication != null && authentication.getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch("ROLE_ADMIN"::equals)) {
            return true;
        }

        if (adminWhitelistRepository.isAdmin(login)) {
            return true;
        }

        if (request == null || expectedAdminToken == null || expectedAdminToken.isBlank()) {
            return false;
        }
        String headerValue = request.getHeader(AdminTokenFilter.HEADER);
        return headerValue != null && expectedAdminToken.equals(headerValue);
    }
}
