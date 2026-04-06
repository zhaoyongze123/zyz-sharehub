package com.sharehub.resource;

import com.sharehub.common.ApiResponse;
import com.sharehub.common.InMemoryStore;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final InMemoryStore store;

    public ResourceController(InMemoryStore store) {
        this.store = store;
    }

    @PostMapping
    public ApiResponse<ResourceDto> create(@Valid @RequestBody ResourceDto req) {
        long id = store.nextId();
        ResourceDto saved = new ResourceDto(id, req.title(), req.type(), req.summary(), req.tags(), req.externalUrl(), req.objectKey(), req.visibility(), "DRAFT");
        store.resources.put(id, saved);
        return ApiResponse.ok(saved);
    }

    @GetMapping
    public ApiResponse<List<Object>> list() {
        return ApiResponse.ok(new ArrayList<>(store.resources.values()));
    }

    @GetMapping("/{id}")
    public ApiResponse<Object> detail(@PathVariable Long id) {
        return ApiResponse.ok(store.resources.get(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<ResourceDto> update(@PathVariable Long id, @Valid @RequestBody ResourceDto req) {
        ResourceDto saved = new ResourceDto(id, req.title(), req.type(), req.summary(), req.tags(), req.externalUrl(), req.objectKey(), req.visibility(), req.status() == null ? "DRAFT" : req.status());
        store.resources.put(id, saved);
        return ApiResponse.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        store.resources.remove(id);
        return ApiResponse.ok("DELETED");
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<Object> publish(@PathVariable Long id) {
        Object data = store.resources.get(id);
        return ApiResponse.ok(data);
    }
}
