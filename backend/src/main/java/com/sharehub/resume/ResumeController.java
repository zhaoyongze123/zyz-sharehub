package com.sharehub.resume;

import com.sharehub.auth.RequestAccessService;
import com.sharehub.common.ApiResponse;
import com.sharehub.common.NotFoundException;
import com.sharehub.common.PageResponse;
import com.sharehub.files.FileCategory;
import com.sharehub.files.FileRecord;
import com.sharehub.files.FileRepository;
import com.sharehub.files.FileStorageService;
import com.sharehub.files.StoredFileDto;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    private final RequestAccessService requestAccessService;

    public ResumeController(
        ResumeRepository resumeRepository,
        FileStorageService fileStorageService,
        FileRepository fileRepository,
        RequestAccessService requestAccessService
    ) {
        this.resumeRepository = resumeRepository;
        this.fileStorageService = fileStorageService;
        this.fileRepository = fileRepository;
        this.requestAccessService = requestAccessService;
    }

    @PostMapping("/generate")
    public ApiResponse<ResumeDto> generate(
        Authentication authentication,
        HttpServletRequest request,
        @RequestBody Map<String, Object> req
    ) {
        String ownerKey = requestAccessService.requireUser(authentication, request);
        String templateKey = String.valueOf(req.getOrDefault("templateKey", "default"));
        byte[] pdfBytes = fileStorageService.buildSimplePdf("ShareHub Resume", "template=" + templateKey);
        StoredFileDto storedFile = fileStorageService.storeBytes(
            ownerKey,
            FileCategory.RESUME_PDF,
            "RESUME",
            "pending",
            "resume-" + templateKey + ".pdf",
            MediaType.APPLICATION_PDF_VALUE,
            pdfBytes
        );
        return ApiResponse.ok(resumeRepository.create(ownerKey, templateKey, storedFile.id()));
    }

    @GetMapping
    public ApiResponse<PageResponse<ResumeDto>> list(
        Authentication authentication,
        HttpServletRequest request,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String templateKey,
        @RequestParam(required = false) String keyword
    ) {
        String ownerKey = requestAccessService.requireUser(authentication, request);
        return ApiResponse.ok(resumeRepository.list(
            ownerKey,
            page,
            pageSize,
            normalize(status),
            normalize(templateKey),
            normalize(keyword)
        ));
    }

    @GetMapping("/workbench")
    public ApiResponse<ResumeWorkbenchDto> workbench(Authentication authentication, HttpServletRequest request) {
        String ownerKey = requestAccessService.requireUser(authentication, request);
        return ApiResponse.ok(resumeRepository.workbench(ownerKey));
    }

    @GetMapping("/{id}")
    public ApiResponse<ResumeDto> detail(Authentication authentication, HttpServletRequest request, @PathVariable Long id) {
        String ownerKey = requestAccessService.requireUser(authentication, request);
        return ApiResponse.ok(resumeRepository.findOwned(id, ownerKey));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(Authentication authentication, HttpServletRequest request, @PathVariable Long id) {
        String ownerKey = requestAccessService.requireUser(authentication, request);
        ResumeDto resume = resumeRepository.findOwned(id, ownerKey);
        if (resume.fileId() != null) {
            fileRepository.deleteById(resume.fileId());
        }
        resumeRepository.delete(id, ownerKey);
        return ApiResponse.ok("DELETED");
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(Authentication authentication, HttpServletRequest request, @PathVariable Long id) {
        String ownerKey = requestAccessService.requireUser(authentication, request);
        ResumeDto resume = resumeRepository.findOwned(id, ownerKey);
        if (resume.fileId() == null) {
            throw new NotFoundException("RESUME_FILE_NOT_FOUND");
        }
        Optional<FileRecord> record = fileStorageService.load(resume.fileId());
        if (record.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        FileRecord file = record.get();
        return ResponseEntity.ok()
            .contentType(fileStorageService.resolveMediaType(file.getContentType()))
            .header("Content-Disposition", "attachment; filename=\"" + file.getFilename() + "\"")
            .body(file.getData());
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
