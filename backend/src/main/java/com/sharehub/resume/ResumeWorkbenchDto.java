package com.sharehub.resume;

import java.util.List;

public record ResumeWorkbenchDto(
    long total,
    long generatedCount,
    List<ResumeTemplateBreakdownDto> templateBreakdown,
    List<ResumeDto> recentItems
) {
}
