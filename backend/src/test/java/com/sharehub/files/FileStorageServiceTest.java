package com.sharehub.files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    private FileRepository repository;

    private FileStorageProperties properties;

    @InjectMocks
    private FileStorageService service;

    @BeforeEach
    void setUp() {
        properties = new FileStorageProperties();
        properties.setMaxSize(5);
        service = new FileStorageService(repository, properties);
    }

    @Test
    void storeBytesPersistsRecordAndReturnsDto() {
        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);

        when(repository.save(any(FileRecord.class))).thenAnswer(invocation -> {
            FileRecord record = invocation.getArgument(0);
            record.setId(UUID.randomUUID());
            return record;
        });

        StoredFileDto dto = service.storeBytes(
            "user-1",
            FileCategory.RESUME_PDF,
            "RESUME",
            "resume-42",
            "resume.pdf",
            "application/pdf",
            data
        );

        assertThat(dto.id()).isNotNull();
        assertThat(dto.downloadUrl()).isEqualTo("/api/files/" + dto.id());
        assertThat(dto.size()).isEqualTo(data.length);
        assertThat(dto.checksum()).hasSize(64);
    }

    @Test
    void storeBytesRejectsBlankOwner() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            service.storeBytes("  ", FileCategory.AVATAR, "USER", "1", "avatar.png", "image/png", new byte[]{1})
        );

        assertThat(exception.getStatusCode().value()).isEqualTo(400);
        assertThat(exception.getReason()).isEqualTo("FILE_OWNER_REQUIRED");
    }

    @Test
    void storeBytesRejectsMissingReference() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            service.storeBytes("user", FileCategory.AVATAR, "", " ", "avatar.png", "image/png", new byte[]{1})
        );

        assertThat(exception.getStatusCode().value()).isEqualTo(400);
        assertThat(exception.getReason()).isEqualTo("FILE_REFERENCE_REQUIRED");
    }

    @Test
    void storeBytesRejectsBlankFilename() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            service.storeBytes("user", FileCategory.AVATAR, "USER", "1", " ", "image/png", new byte[]{1})
        );

        assertThat(exception.getStatusCode().value()).isEqualTo(400);
        assertThat(exception.getReason()).isEqualTo("FILE_NAME_REQUIRED");
    }

    @Test
    void storeMultipartRejectsBlankOriginalFilename() {
        MultipartFile file = mock(MultipartFile.class);

        when(file.getOriginalFilename()).thenReturn(" ");
        when(file.getContentType()).thenReturn("image/png");
        try {
            when(file.getBytes()).thenReturn(new byte[]{1});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            service.storeMultipart("user", FileCategory.AVATAR, "USER", "1", file)
        );

        assertThat(exception.getStatusCode().value()).isEqualTo(400);
        assertThat(exception.getReason()).isEqualTo("FILE_NAME_REQUIRED");
    }

    @Test
    void storeBytesRejectsEmptyData() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            service.storeBytes("user", FileCategory.AVATAR, "USER", "1", "avatar.png", "image/png", new byte[0])
        );

        assertThat(exception.getStatusCode().value()).isEqualTo(400);
        assertThat(exception.getReason()).isEqualTo("FILE_EMPTY");
    }

    @Test
    void storeBytesRejectsOversizedFile() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            service.storeBytes("user", FileCategory.AVATAR, "USER", "1", "avatar.png", "image/png", new byte[]{1, 2, 3, 4, 5, 6})
        );

        assertThat(exception.getStatusCode().value()).isEqualTo(400);
        assertThat(exception.getReason()).isEqualTo("FILE_TOO_LARGE");
    }
}
