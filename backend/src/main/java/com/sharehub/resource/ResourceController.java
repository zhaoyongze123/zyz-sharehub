package com.sharehub.resource;

import com.sharehub.common.ApiResponse;
import com.sharehub.common.NotFoundException;
import com.sharehub.common.PageResponse;
import com.sharehub.files.FileCategory;
import com.sharehub.files.FileStorageService;
import com.sharehub.files.StoredFileDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceRepository repository;
    private final FileStorageService fileStorageService;

    public ResourceController(ResourceRepository repository, FileStorageService fileStorageService) {
        this.repository = repository;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping
    public ApiResponse<ResourceDto> create(@Valid @RequestBody ResourceDto req) {
        ResourceEntity entity = new ResourceEntity();
        entity.setTitle(req.title());
        entity.setType(req.type());
        entity.setSummary(req.summary());
        entity.setTags(req.tags());
        entity.setExternalUrl(req.externalUrl());
        entity.setObjectKey(req.objectKey());
        entity.setOwnerKey("local-dev-user");
        entity.setVisibility(req.visibility());
        entity.setStatus("DRAFT");
        return ApiResponse.ok(repository.save(entity).toDto());
    }

    @GetMapping
    public ApiResponse<PageResponse<ResourceDto>> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int pageSize,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String visibility
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, pageSize);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<ResourceEntity> result = repository.findByStatusAndVisibility(status, visibility, pageable);
        return ApiResponse.ok(PageResponse.from(result.map(ResourceEntity::toDto)));
    }

    @GetMapping("/featured")
    public ApiResponse<List<ResourceDto>> featured() {
        return ApiResponse.ok(repository.findTop6ByPublishedOrderByUpdatedAtDesc().stream().map(ResourceEntity::toDto).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<ResourceDto> detail(@PathVariable Long id) {
        return repository.findById(id)
            .map(ResourceEntity::toDto)
            .map(ApiResponse::ok)
            .orElseThrow(() -> new NotFoundException("NOT_FOUND"));
    }

    @PutMapping("/{id}")
    public ApiResponse<ResourceDto> update(@PathVariable Long id, @Valid @RequestBody ResourceDto req) {
        return repository.findById(id)
            .map(existing -> {
                existing.setTitle(req.title());
                existing.setType(req.type());
                existing.setSummary(req.summary());
                existing.setTags(req.tags());
                existing.setExternalUrl(req.externalUrl());
                existing.setObjectKey(req.objectKey());
                existing.setVisibility(req.visibility());
                existing.setStatus(req.status() == null ? existing.getStatus() : req.status());
                return ApiResponse.ok(repository.save(existing).toDto());
            })
            .orElseThrow(() -> new NotFoundException("NOT_FOUND"));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        repository.deleteById(id);
        return ApiResponse.ok("DELETED");
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<ResourceDto> publish(@PathVariable Long id) {
        return repository.findById(id)
            .map(entity -> {
                entity.setStatus("PUBLISHED");
                return ApiResponse.ok(repository.save(entity).toDto());
            })
            .orElseThrow(() -> new NotFoundException("NOT_FOUND"));
    }

    @PostMapping(path = "/{id}/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> uploadAttachment(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return repository.findById(id)
            .map(entity -> {
                StoredFileDto stored = fileStorageService.storeMultipart(
                    "resource:" + id,
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
            })
            .orElseThrow(() -> new NotFoundException("RESOURCE_NOT_FOUND"));
    }
}
