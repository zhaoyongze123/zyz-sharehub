package com.sharehub.note;

import com.sharehub.common.ApiResponse;
import com.sharehub.common.InMemoryStore;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final InMemoryStore store;

    public NoteController(InMemoryStore store) {
        this.store = store;
    }

    @PostMapping
    public ApiResponse<NoteDto> create(@Valid @RequestBody NoteDto req) {
        long id = store.nextId();
        NoteDto saved = new NoteDto(id, req.title(), req.contentMd(), req.visibility(), "PUBLISHED");
        store.notes.put(id, saved);
        return ApiResponse.ok(saved);
    }

    @GetMapping
    public ApiResponse<List<Object>> list() {
        return ApiResponse.ok(new ArrayList<>(store.notes.values()));
    }

    @GetMapping("/{id}")
    public ApiResponse<Object> detail(@PathVariable Long id) {
        return ApiResponse.ok(store.notes.get(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<NoteDto> update(@PathVariable Long id, @Valid @RequestBody NoteDto req) {
        NoteDto saved = new NoteDto(id, req.title(), req.contentMd(), req.visibility(), req.status());
        store.notes.put(id, saved);
        return ApiResponse.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        store.notes.remove(id);
        return ApiResponse.ok("DELETED");
    }
}
