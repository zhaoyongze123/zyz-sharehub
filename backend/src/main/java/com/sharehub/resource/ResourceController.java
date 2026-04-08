package com.sharehub.resource;

import com.sharehub.auth.UserProfileDto;
import com.sharehub.auth.UserProfileRepository;
import com.sharehub.auth.RequestAccessService;
import com.sharehub.common.ApiResponse;
import com.sharehub.common.NotFoundException;
import com.sharehub.common.PageResponse;
import com.sharehub.files.FileCategory;
import com.sharehub.files.FileStorageService;
import com.sharehub.files.StoredFileDto;
import com.sharehub.interaction.InteractionRepository;
import com.sharehub.interaction.InteractionSummaryDto;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceRepository repository;
    private final FileStorageService fileStorageService;
    private final InteractionRepository interactionRepository;
    private final UserProfileRepository userProfileRepository;
    private final RequestAccessService requestAccessService;

    public ResourceController(
        ResourceRepository repository,
        FileStorageService fileStorageService,
        InteractionRepository interactionRepository,
        UserProfileRepository userProfileRepository,
        RequestAccessService requestAccessService
    ) {
        this.repository = repository;
        this.fileStorageService = fileStorageService;
        this.interactionRepository = interactionRepository;
        this.userProfileRepository = userProfileRepository;
        this.requestAccessService = requestAccessService;
    }

    @PostMapping
    public ApiResponse<ResourceDto> create(
        Authentication authentication,
        HttpServletRequest request,
        @Valid @RequestBody ResourceDto req
    ) {
        String ownerKey = requestAccessService.requireUser(authentication, request);
        ResourceEntity entity = new ResourceEntity();
        entity.setTitle(req.title());
        entity.setType(req.category() == null || req.category().isBlank() ? req.type() : req.category());
        entity.setSummary(req.summary());
        entity.setTags(req.tags());
        entity.setExternalUrl(req.externalUrl());
        entity.setObjectKey(req.objectKey());
        entity.setOwnerKey(ownerKey);
        entity.setVisibility(req.visibility());
        entity.setStatus("DRAFT");
        return ApiResponse.ok(enrichResource(repository.save(entity)));
    }

    @GetMapping
    public ApiResponse<PageResponse<ResourceDto>> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int pageSize,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String visibility,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String tag,
        @RequestParam(defaultValue = "latest") String sortBy
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, pageSize);
        List<String> statuses = parseStatuses(status);
        String normalizedKeyword = normalize(keyword);
        String normalizedCategory = normalize(category);
        String normalizedTag = normalize(tag);
        String normalizedVisibility = normalize(visibility);

        if ("hot".equalsIgnoreCase(sortBy)) {
            Page<ResourceEntity> allCandidates = repository.findVisibleByFiltersOrderByUpdatedAtDesc(
                statuses,
                normalizedKeyword,
                normalizedCategory,
                normalizedTag,
                normalizedVisibility,
                PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "updatedAt"))
            );
            List<ResourceDto> sorted = enrichResources(allCandidates.getContent()).stream()
                .sorted(
                    Comparator.comparingLong(ResourceDto::likes).reversed()
                        .thenComparing(ResourceDto::updatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                )
                .toList();
            long total = sorted.size();
            int fromIndex = Math.min(safePage * safeSize, sorted.size());
            int toIndex = Math.min(fromIndex + safeSize, sorted.size());
            return ApiResponse.ok(PageResponse.of(sorted.subList(fromIndex, toIndex), safePage, safeSize, total));
        }

        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<ResourceEntity> result = repository.findVisibleByFiltersOrderByUpdatedAtDesc(
            statuses,
            normalizedKeyword,
            normalizedCategory,
            normalizedTag,
            normalizedVisibility,
            pageable
        );
        long total = repository.countByVisibleFilters(statuses, normalizedKeyword, normalizedCategory, normalizedTag, normalizedVisibility);
        return ApiResponse.ok(PageResponse.of(enrichResources(result.getContent()), safePage, safeSize, total));
    }

    @GetMapping("/featured")
    public ApiResponse<List<ResourceDto>> featured() {
        return ApiResponse.ok(enrichResources(repository.findTop6ByPublishedOrderByUpdatedAtDesc()).stream().limit(6).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<ResourceDto> detail(@PathVariable Long id) {
        return repository.findById(id)
            .map(entity -> {
                if ("REMOVED".equalsIgnoreCase(entity.getStatus())) {
                    throw new ResponseStatusException(HttpStatus.GONE, "RESOURCE_REMOVED");
                }
                return entity;
            })
            .map(this::enrichResource)
            .map(ApiResponse::ok)
            .orElseThrow(() -> new NotFoundException("NOT_FOUND"));
    }

    @GetMapping("/{id}/related")
    public ApiResponse<List<ResourceDto>> related(@PathVariable Long id) {
        ResourceEntity source = repository.findById(id)
            .orElseThrow(() -> new NotFoundException("RESOURCE_NOT_FOUND"));
        Set<String> sourceTags = source.getTags().stream()
            .map(String::trim)
            .filter(tag -> !tag.isEmpty())
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
        List<ResourceDto> related = enrichResources(repository.findPublishedExcludingId(id)).stream()
            .filter(item -> isRelated(source, sourceTags, item))
            .sorted(
                Comparator.comparingInt((ResourceDto item) -> relatedScore(source, sourceTags, item)).reversed()
                    .thenComparing(ResourceDto::updatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            )
            .limit(4)
            .toList();
        return ApiResponse.ok(related);
    }

    @PutMapping("/{id}")
    public ApiResponse<ResourceDto> update(
        Authentication authentication,
        HttpServletRequest request,
        @PathVariable Long id,
        @Valid @RequestBody ResourceDto req
    ) {
        String ownerKey = requestAccessService.requireUser(authentication, request);
        ResourceEntity existing = requireOwnedResource(id, ownerKey);
        existing.setTitle(req.title());
        existing.setType(req.category() == null || req.category().isBlank() ? req.type() : req.category());
        existing.setSummary(req.summary());
        existing.setTags(req.tags());
        existing.setExternalUrl(req.externalUrl());
        existing.setObjectKey(req.objectKey());
        existing.setVisibility(req.visibility());
        existing.setStatus(req.status() == null ? existing.getStatus() : req.status());
        return ApiResponse.ok(enrichResource(repository.save(existing)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(
        Authentication authentication,
        HttpServletRequest request,
        @PathVariable Long id
    ) {
        String ownerKey = requestAccessService.requireUser(authentication, request);
        ResourceEntity entity = requireOwnedResource(id, ownerKey, "RESOURCE_NOT_FOUND");
        repository.delete(entity);
        return ApiResponse.ok("DELETED");
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<ResourceDto> publish(
        Authentication authentication,
        HttpServletRequest request,
        @PathVariable Long id
    ) {
        String ownerKey = requestAccessService.requireUser(authentication, request);
        ResourceEntity entity = requireOwnedResource(id, ownerKey);
        entity.setStatus("PUBLISHED");
        return ApiResponse.ok(enrichResource(repository.save(entity)));
    }

    private List<String> parseStatuses(String status) {
        if (status == null || status.isBlank()) {
            return List.of("PUBLISHED");
        }
        return Arrays.stream(status.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }

    private List<ResourceDto> enrichResources(List<ResourceEntity> resources) {
        Map<Long, InteractionSummaryDto> summaries = interactionRepository.summarizeResources(
            resources.stream().map(ResourceEntity::getId).collect(Collectors.toSet())
        );
        return resources.stream()
            .map(resource -> toDto(resource, summaries.get(resource.getId())))
            .toList();
    }

    private ResourceDto enrichResource(ResourceEntity resource) {
        return toDto(resource, interactionRepository.summarizeResource(resource.getId()));
    }

    private ResourceDto toDto(ResourceEntity resource, InteractionSummaryDto summary) {
        Optional<UserProfileDto> profile = userProfileRepository.findOptionalByLogin(resource.getOwnerKey());
        String author = profile.map(item -> item.name() == null || item.name().isBlank() ? item.login() : item.name())
            .orElse(resource.getOwnerKey());
        long likes = summary == null ? 0L : summary.likes();
        long favorites = summary == null ? 0L : summary.favorites();
        long downloadCount = resource.getObjectKey() == null || resource.getObjectKey().isBlank() ? 0L : 1L;
        return resource.toDto(author, likes, favorites, downloadCount);
    }

    private boolean isRelated(ResourceEntity source, Set<String> sourceTags, ResourceDto item) {
        if (source.getType() != null && source.getType().equalsIgnoreCase(item.type())) {
            return true;
        }
        if (sourceTags.isEmpty()) {
            return false;
        }
        Set<String> candidateTags = new HashSet<>(item.tags().stream().map(String::toLowerCase).toList());
        candidateTags.retainAll(sourceTags);
        return !candidateTags.isEmpty();
    }

    private int relatedScore(ResourceEntity source, Set<String> sourceTags, ResourceDto item) {
        int score = 0;
        if (source.getType() != null && source.getType().equalsIgnoreCase(item.type())) {
            score += 3;
        }
        for (String tag : item.tags()) {
            if (sourceTags.contains(tag.toLowerCase())) {
                score += 1;
            }
        }
        return score;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    @PostMapping(path = "/{id}/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> uploadAttachment(
        Authentication authentication,
        HttpServletRequest request,
        @PathVariable Long id,
        @RequestPart("file") MultipartFile file
    ) {
        String ownerKey = requestAccessService.requireUser(authentication, request);
        ResourceEntity entity = requireOwnedResource(id, ownerKey, "RESOURCE_NOT_FOUND");
        StoredFileDto stored = fileStorageService.storeMultipart(
            ownerKey,
            FileCategory.RESOURCE_ATTACHMENT,
            "RESOURCE",
            String.valueOf(id),
            file
        );
        entity.setObjectKey(stored.id().toString());
        repository.save(entity);

        Map<String, Object> result = new HashMap<>();
        result.put("resourceId", id);
        result.put("file", stored);
        return ApiResponse.ok(result);
    }

    private ResourceEntity requireOwnedResource(Long id, String ownerKey) {
        return requireOwnedResource(id, ownerKey, "NOT_FOUND");
    }

    private ResourceEntity requireOwnedResource(Long id, String ownerKey, String notFoundCode) {
        ResourceEntity entity = repository.findById(id)
            .orElseThrow(() -> new NotFoundException(notFoundCode));
        if (!entity.getOwnerKey().equals(ownerKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "RESOURCE_FORBIDDEN");
        }
        return entity;
    }
}
