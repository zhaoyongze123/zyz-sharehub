package com.sharehub.files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

        ResponseEntity<byte[]> response = controller.download(id);

        assertThat(response.getHeaders().getContentType()).hasToString("application/octet-stream");
        assertThat(response.getHeaders().getFirst("Content-Disposition"))
            .isEqualTo("attachment; filename=\"resume.pdf\"");
        assertThat(response.getBody()).containsExactly(1, 2);
    }
}
