package com.sharehub.note;

import com.sharehub.auth.RequestAccessService;
import com.sharehub.auth.UserProfileRepository;
import com.sharehub.common.ApiResponse;
import com.sharehub.common.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteRepository repository;
    private final RequestAccessService requestAccessService;
    private final UserProfileRepository userProfileRepository;

    public NoteController(
        NoteRepository repository,
        RequestAccessService requestAccessService,
        UserProfileRepository userProfileRepository
    ) {
        this.repository = repository;
        this.requestAccessService = requestAccessService;
        this.userProfileRepository = userProfileRepository;
    }

    @PostMapping
    public ApiResponse<NoteDto> create(
        Authentication authentication,
        HttpServletRequest request,
        @Valid @RequestBody NoteDto req
    ) {
        String ownerKey = requireActiveUser(authentication, request);
        NoteDto toSave = new NoteDto(null, req.title(), req.contentMd(), req.visibility(), req.status());
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
        return ApiResponse.ok(repository.findAccessible(id, ownerKey));
    }

    @PutMapping("/{id}")
    public ApiResponse<NoteDto> update(
        Authentication authentication,
        HttpServletRequest request,
        @PathVariable Long id,
        @Valid @RequestBody NoteDto req
    ) {
        String ownerKey = requireActiveUser(authentication, request);
        NoteDto updated = repository.upsertOwned(id, ownerKey, req);
        return ApiResponse.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(
        Authentication authentication,
        HttpServletRequest request,
        @PathVariable Long id
    ) {
        String ownerKey = requireActiveUser(authentication, request);
        repository.deleteOwned(id, ownerKey);
        return ApiResponse.ok("DELETED");
    }

    private String requireActiveUser(Authentication authentication, HttpServletRequest request) {
        String login = requestAccessService.requireUser(authentication, request);
        userProfileRepository.upsert(login, login, null);
        userProfileRepository.ensureActive(login);
        return login;
    }
}
