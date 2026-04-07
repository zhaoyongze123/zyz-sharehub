package com.sharehub.resume;

import com.sharehub.common.ApiResponse;
import com.sharehub.common.NotFoundException;
import com.sharehub.common.PageResponse;
import com.sharehub.files.FileCategory;
import com.sharehub.files.FileRecord;
import com.sharehub.files.FileRepository;
import com.sharehub.files.FileStorageService;
import com.sharehub.files.StoredFileDto;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeRepository resumeRepository;
    private final FileStorageService fileStorageService;
    private final FileRepository fileRepository;

    public ResumeController(ResumeRepository resumeRepository, FileStorageService fileStorageService, FileRepository fileRepository) {
        this.resumeRepository = resumeRepository;
        this.fileStorageService = fileStorageService;
        this.fileRepository = fileRepository;
    }

    @PostMapping("/generate")
    public ApiResponse<ResumeDto> generate(@RequestBody Map<String, Object> req) {
        String templateKey = String.valueOf(req.getOrDefault("templateKey", "default"));
        byte[] pdfBytes = fileStorageService.buildSimplePdf("ShareHub Resume", "template=" + templateKey);
        StoredFileDto storedFile = fileStorageService.storeBytes(
            "resume:pending",
            FileCategory.RESUME_PDF,
            "RESUME",
            "pending",
            "resume-" + templateKey + ".pdf",
            MediaType.APPLICATION_PDF_VALUE,
            pdfBytes
        );
        return ApiResponse.ok(resumeRepository.create(templateKey, storedFile.id()));
    }

    @GetMapping
    public ApiResponse<PageResponse<ResumeDto>> list(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) String status
    ) {
        return ApiResponse.ok(resumeRepository.list("local-dev-user", page, pageSize, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<ResumeDto> detail(@PathVariable Long id) {
        return ApiResponse.ok(resumeRepository.findOwned(id, "local-dev-user"));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        ResumeDto resume = resumeRepository.findOwned(id, "local-dev-user");
        if (resume.fileId() != null) {
            fileRepository.deleteById(resume.fileId());
        }
        resumeRepository.delete(id, "local-dev-user");
        return ApiResponse.ok("DELETED");
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        ResumeDto resume = resumeRepository.findOwned(id, "local-dev-user");
        if (resume.fileId() == null) {
            throw new NotFoundException("RESUME_FILE_NOT_FOUND");
        }
        Optional<FileRecord> record = fileStorageService.load(resume.fileId());
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
