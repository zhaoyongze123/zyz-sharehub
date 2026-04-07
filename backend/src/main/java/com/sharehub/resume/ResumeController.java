package com.sharehub.resume;

import com.sharehub.common.ApiResponse;
import com.sharehub.common.NotFoundException;
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

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeRepository resumeRepository;
    private final FileStorageService fileStorageService;

    public ResumeController(ResumeRepository resumeRepository, FileStorageService fileStorageService) {
        this.resumeRepository = resumeRepository;
        this.fileStorageService = fileStorageService;
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
        ResumeDto saved = resumeRepository.create(templateKey, storedFile.id());
        return ApiResponse.ok(saved);
    }

    @GetMapping("/{id}")
    public ApiResponse<ResumeDto> detail(@PathVariable Long id) {
        return ApiResponse.ok(resumeRepository.find(id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        ResumeDto resume = resumeRepository.find(id);
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
