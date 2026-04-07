package com.sharehub.resume;

import com.sharehub.common.ApiResponse;
import com.sharehub.common.InMemoryStore;
import com.sharehub.files.FileCategory;
import com.sharehub.files.FileRecord;
import com.sharehub.files.FileStorageService;
import com.sharehub.files.StoredFileDto;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final InMemoryStore store;
    private final FileStorageService fileStorageService;

    public ResumeController(InMemoryStore store, FileStorageService fileStorageService) {
        this.store = store;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/generate")
    public ApiResponse<Map<String, Object>> generate(@RequestBody Map<String, Object> req) {
        long id = store.nextId();
        String templateKey = String.valueOf(req.getOrDefault("templateKey", "default"));
        byte[] pdfBytes = fileStorageService.buildSimplePdf("ShareHub Resume #" + id, "template=" + templateKey);
        StoredFileDto storedFile = fileStorageService.storeBytes(
            "resume:" + id,
            FileCategory.RESUME_PDF,
            "RESUME",
            String.valueOf(id),
            "resume-" + id + ".pdf",
            MediaType.APPLICATION_PDF_VALUE,
            pdfBytes
        );

        Map<String, Object> saved = new HashMap<>();
        saved.put("id", id);
        saved.put("templateKey", templateKey);
        saved.put("status", "GENERATED");
        saved.put("fileId", storedFile.id());
        saved.put("fileUrl", "/api/resumes/" + id + "/download");
        store.resumes.put(id, saved);
        return ApiResponse.ok(saved);
    }

    @GetMapping("/{id}")
    public ApiResponse<Object> detail(@PathVariable Long id) {
        return ApiResponse.ok(store.resumes.get(id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        Object found = store.resumes.get(id);
        if (!(found instanceof Map<?, ?> resumeMap)) {
            return ResponseEntity.notFound().build();
        }

        Object fileId = resumeMap.get("fileId");
        if (fileId == null) {
            return ResponseEntity.notFound().build();
        }

        Optional<FileRecord> record = fileStorageService.load(UUID.fromString(String.valueOf(fileId)));
        if (record.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        FileRecord file = record.get();
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header("Content-Disposition", "attachment; filename=\"" + file.getFilename() + "\"")
            .body(file.getData());
    }
}
