package com.sharehub.files;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileStorageService {

    private final FileRepository repository;
    private final FileStorageProperties properties;

    public FileStorageService(FileRepository repository, FileStorageProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public StoredFileDto storeMultipart(String owner, FileCategory category, String referenceType, String referenceId, MultipartFile file) {
        try {
            return storeBytes(
                owner,
                category,
                referenceType,
                referenceId,
                defaultFilename(file.getOriginalFilename()),
                defaultContentType(file.getContentType()),
                file.getBytes()
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FILE_READ_ERROR", exception);
        }
    }

    public StoredFileDto storeBytes(
        String owner,
        FileCategory category,
        String referenceType,
        String referenceId,
        String filename,
        String contentType,
        byte[] data
    ) {
        validate(owner, referenceType, referenceId, filename, data);

        FileRecord record = new FileRecord();
        record.setOwner(owner);
        record.setCategory(category);
        record.setReferenceType(referenceType);
        record.setReferenceId(referenceId);
        record.setFilename(filename);
        record.setContentType(defaultContentType(contentType));
        record.setSize(data.length);
        record.setChecksum(sha256(data));
        record.setData(data);
        return toDto(repository.save(record));
    }

    public Optional<FileRecord> load(UUID id) {
        return repository.findById(id);
    }

    public StoredFileDto toDto(FileRecord record) {
        return new StoredFileDto(
            record.getId(),
            record.getOwner(),
            record.getCategory(),
            record.getReferenceType(),
            record.getReferenceId(),
            record.getFilename(),
            record.getContentType(),
            record.getSize(),
            record.getChecksum(),
            "/api/files/" + record.getId(),
            record.getCreatedAt()
        );
    }

    public byte[] buildSimplePdf(String title, String body) {
        String safeTitle = sanitizePdfText(title);
        String safeBody = sanitizePdfText(body);
        String content = "BT /F1 18 Tf 72 740 Td (" + safeTitle + ") Tj 0 -28 Td /F1 12 Tf (" + safeBody + ") Tj ET";
        String pdf = "%PDF-1.4\n"
            + "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n"
            + "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n"
            + "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >> endobj\n"
            + "4 0 obj << /Length " + content.getBytes(StandardCharsets.US_ASCII).length + " >> stream\n"
            + content + "\nendstream endobj\n"
            + "5 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n"
            + "xref\n0 6\n"
            + "0000000000 65535 f \n"
            + "0000000010 00000 n \n"
            + "0000000063 00000 n \n"
            + "0000000122 00000 n \n"
            + "0000000248 00000 n \n"
            + "0000000397 00000 n \n"
            + "trailer << /Root 1 0 R /Size 6 >>\n"
            + "startxref\n470\n%%EOF";
        return pdf.getBytes(StandardCharsets.US_ASCII);
    }

    private void validate(String owner, String referenceType, String referenceId, String filename, byte[] data) {
        if (owner == null || owner.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FILE_OWNER_REQUIRED");
        }
        if (referenceType == null || referenceType.isBlank() || referenceId == null || referenceId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FILE_REFERENCE_REQUIRED");
        }
        if (filename == null || filename.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FILE_NAME_REQUIRED");
        }
        if (data == null || data.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FILE_EMPTY");
        }
        if (data.length > properties.getMaxSize()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FILE_TOO_LARGE");
        }
    }

    private String defaultFilename(String filename) {
        return (filename == null || filename.isBlank()) ? "upload.bin" : filename;
    }

    private String defaultContentType(String contentType) {
        return (contentType == null || contentType.isBlank()) ? "application/octet-stream" : contentType;
    }

    private String sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256_NOT_AVAILABLE", exception);
        }
    }

    private String sanitizePdfText(String text) {
        String value = (text == null || text.isBlank()) ? "-" : text;
        return value
            .replace("\\", "\\\\")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replaceAll("[^\\x20-\\x7E]", " ");
    }
}
