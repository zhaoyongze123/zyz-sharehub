package com.sharehub.files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock
    private FileStorageService storageService;

    @InjectMocks
    private FileController controller;

    @Test
    void downloadFallsBackToOctetStreamWhenContentTypeMissing() {
        UUID id = UUID.randomUUID();
        FileRecord record = new FileRecord();
        record.setId(id);
        record.setFilename("resume.pdf");
        record.setData(new byte[]{1, 2});
        record.setContentType(null);

        when(storageService.load(id)).thenReturn(Optional.of(record));
        when(storageService.resolveMediaType(record.getContentType())).thenReturn(MediaType.APPLICATION_OCTET_STREAM);

        ResponseEntity<byte[]> response = controller.download(id);

        assertThat(response.getHeaders().getContentType()).hasToString("application/octet-stream");
        assertThat(response.getHeaders().getFirst("Content-Disposition"))
            .isEqualTo("attachment; filename=\"resume.pdf\"");
        assertThat(response.getBody()).containsExactly(1, 2);
    }

    @Test
    void downloadFallsBackToOctetStreamWhenContentTypeBlank() {
        UUID id = UUID.randomUUID();
        FileRecord record = new FileRecord();
        record.setId(id);
        record.setFilename("resume.pdf");
        record.setData(new byte[]{1, 2});
        record.setContentType("   ");

        when(storageService.load(id)).thenReturn(Optional.of(record));
        when(storageService.resolveMediaType(record.getContentType())).thenReturn(MediaType.APPLICATION_OCTET_STREAM);

        ResponseEntity<byte[]> response = controller.download(id);

        assertThat(response.getHeaders().getContentType()).hasToString("application/octet-stream");
        assertThat(response.getHeaders().getFirst("Content-Disposition"))
            .isEqualTo("attachment; filename=\"resume.pdf\"");
        assertThat(response.getBody()).containsExactly(1, 2);
    }

    @Test
    void downloadFallsBackToOctetStreamWhenContentTypeInvalid() {
        UUID id = UUID.randomUUID();
        FileRecord record = new FileRecord();
        record.setId(id);
        record.setFilename("resume.pdf");
        record.setData(new byte[]{1, 2});
        record.setContentType("invalid/type;");

        when(storageService.load(id)).thenReturn(Optional.of(record));
        when(storageService.resolveMediaType(record.getContentType())).thenReturn(MediaType.APPLICATION_OCTET_STREAM);

        ResponseEntity<byte[]> response = controller.download(id);

        assertThat(response.getHeaders().getContentType()).hasToString("application/octet-stream");
        assertThat(response.getHeaders().getFirst("Content-Disposition"))
            .isEqualTo("attachment; filename=\"resume.pdf\"");
        assertThat(response.getBody()).containsExactly(1, 2);
    }

    @Test
    void downloadReturnsNotFoundWhenFileMissing() {
        UUID id = UUID.randomUUID();
        when(storageService.load(id)).thenReturn(Optional.empty());

        ResponseEntity<byte[]> response = controller.download(id);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNull();
    }
}
