package com.sharehub.resume;

import java.util.UUID;

public record ResumeDto(
    Long id,
    String templateKey,
    String status,
    UUID fileId,
    String fileUrl
) {
}
