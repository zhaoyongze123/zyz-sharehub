package com.sharehub.note;

import com.sharehub.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteRepository repository;

    public NoteController(NoteRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ApiResponse<NoteDto> create(@Valid @RequestBody NoteDto req) {
        NoteDto toSave = new NoteDto(null, req.title(), req.contentMd(), req.visibility(), req.status());
        NoteDto saved = repository.save(toSave);
        return ApiResponse.ok(saved);
    }

    @GetMapping
    public ApiResponse<NoteRepository.NotePage> list(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(repository.list(page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<NoteDto> detail(@PathVariable Long id) {
        return ApiResponse.ok(repository.find(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<NoteDto> update(@PathVariable Long id, @Valid @RequestBody NoteDto req) {
        NoteDto updated = repository.upsert(id, req);
        return ApiResponse.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        repository.delete(id);
        return ApiResponse.ok("DELETED");
    }
}
