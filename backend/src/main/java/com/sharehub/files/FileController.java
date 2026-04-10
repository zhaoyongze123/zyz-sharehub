package com.sharehub.files;

import com.sharehub.common.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService storageService;

    public FileController(FileStorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<StoredFileDto> upload(@RequestParam("owner") String owner,
                                             @RequestParam("category") FileCategory category,
                                             @RequestParam("referenceType") String referenceType,
                                             @RequestParam("referenceId") String referenceId,
                                             @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(storageService.storeMultipart(owner, category, referenceType, referenceId, file));
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) {
        Optional<FileRecord> record = storageService.load(id);
        if (record.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        FileRecord file = record.get();
        return ResponseEntity.ok()
            .contentType(storageService.resolveMediaType(file.getContentType()))
            .header("Content-Disposition", "attachment; filename=\"" + file.getFilename() + "\"")
            .body(file.getData());
    }
}
