package com.sharehub.me;

import com.sharehub.auth.UserProfileDto;
import com.sharehub.auth.UserProfileRepository;
import com.sharehub.common.PageResponse;
import com.sharehub.interaction.InteractionRepository;
import com.sharehub.note.NoteDto;
import com.sharehub.note.NoteRepository;
import com.sharehub.resource.ResourceDto;
import com.sharehub.resource.ResourceEntity;
import com.sharehub.resource.ResourceRepository;
import com.sharehub.resume.ResumeDto;
import com.sharehub.resume.ResumeRepository;
import com.sharehub.roadmap.RoadmapJdbcRepository;
import com.sharehub.roadmap.RoadmapWorkbenchDto;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class MeService {
    private static final Duration RECENT_RESOURCE_WINDOW = Duration.ofDays(7);
    private static final String PUBLISHED_STATUS = "PUBLISHED";
    private static final String DRAFT_STATUS = "DRAFT";
    private static final String GENERATED_STATUS = "GENERATED";
    private static final RowMapper<MeAggregateMetrics> METRICS_ROW_MAPPER = (resultSet, rowNum) -> new MeAggregateMetrics(
        resultSet.getLong("resource_count"),
        resultSet.getLong("favorite_count"),
        resultSet.getLong("roadmap_count"),
        resultSet.getLong("note_count"),
        resultSet.getLong("resume_count"),
        resultSet.getLong("recent_resource_count"),
        resultSet.getLong("published_resource_count"),
        resultSet.getLong("draft_note_count"),
        resultSet.getLong("generated_resume_count")
    );

    private final UserProfileRepository userProfileRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ResourceRepository resourceRepository;
    private final RoadmapJdbcRepository roadmapJdbcRepository;
    private final NoteRepository noteRepository;
    private final ResumeRepository resumeRepository;
    private final InteractionRepository interactionRepository;

    public MeService(
        UserProfileRepository userProfileRepository,
        JdbcTemplate jdbcTemplate,
        ResourceRepository resourceRepository,
        RoadmapJdbcRepository roadmapJdbcRepository,
        NoteRepository noteRepository,
        ResumeRepository resumeRepository,
        InteractionRepository interactionRepository
    ) {
        this.userProfileRepository = userProfileRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.resourceRepository = resourceRepository;
        this.roadmapJdbcRepository = roadmapJdbcRepository;
        this.noteRepository = noteRepository;
        this.resumeRepository = resumeRepository;
        this.interactionRepository = interactionRepository;
    }

    public MeDto aggregate(String ownerKey) {
        UserProfileDto profile = userProfileRepository.ensureActiveProfile(ownerKey);
        MeAggregateMetrics metrics = jdbcTemplate.queryForObject(
            """
                SELECT
                  (SELECT COUNT(*) FROM resources WHERE owner_key = ?) AS resource_count,
                  (SELECT COUNT(*) FROM favorites WHERE user_key = ?) AS favorite_count,
                  (SELECT COUNT(*) FROM roadmaps WHERE owner_key = ?) AS roadmap_count,
                  (SELECT COUNT(*) FROM notes WHERE owner_key = ?) AS note_count,
                  (SELECT COUNT(*) FROM resumes WHERE owner_key = ?) AS resume_count,
                  (SELECT COUNT(*) FROM resources WHERE owner_key = ? AND created_at >= ?) AS recent_resource_count,
                  (SELECT COUNT(*) FROM resources WHERE owner_key = ? AND status = ?) AS published_resource_count,
                  (SELECT COUNT(*) FROM notes WHERE owner_key = ? AND status = ?) AS draft_note_count,
                  (SELECT COUNT(*) FROM resumes WHERE owner_key = ? AND status = ?) AS generated_resume_count
                """,
            METRICS_ROW_MAPPER,
            ownerKey,
            ownerKey,
            ownerKey,
            ownerKey,
            ownerKey,
            ownerKey,
            Timestamp.from(Instant.now().minus(RECENT_RESOURCE_WINDOW)),
            ownerKey,
            PUBLISHED_STATUS,
            ownerKey,
            DRAFT_STATUS,
            ownerKey,
            GENERATED_STATUS
        );
        MeAggregateMetrics safeMetrics = metrics == null ? MeAggregateMetrics.empty() : metrics;

        return new MeDto(
            profile,
            safeMetrics.resourceCount(),
            safeMetrics.favoriteCount(),
            safeMetrics.roadmapCount(),
            safeMetrics.noteCount(),
            safeMetrics.resumeCount(),
            safeMetrics.recentResourceCount(),
            safeMetrics.publishedResourceCount(),
            safeMetrics.draftNoteCount(),
            safeMetrics.generatedResumeCount()
        );
    }

    public PageResponse<ResourceDto> myResources(String ownerKey, String status, String visibility, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, pageSize);
        Page<ResourceEntity> pageResult = resourceRepository.findByOwnerAndFilters(
            ownerKey,
            normalize(status),
            normalize(visibility),
            PageRequest.of(safePage - 1, safePageSize)
        );
        return PageResponse.of(
            pageResult.getContent().stream().map(ResourceEntity::toDto).toList(),
            safePage,
            safePageSize,
            pageResult.getTotalElements()
        );
    }

    public PageResponse<RoadmapWorkbenchDto> myRoadmaps(String ownerKey, String status, int page, int pageSize) {
        return roadmapJdbcRepository.listWorkbenchByOwner(ownerKey, status, page, pageSize);
    }

    public PageResponse<ResourceDto> myFavorites(String ownerKey, int page, int pageSize) {
        return interactionRepository.listFavoriteResources(ownerKey, page, pageSize);
    }

    public PageResponse<NoteDto> myNotes(String ownerKey, String status, int page, int pageSize) {
        return noteRepository.listByOwner(ownerKey, status, page, pageSize);
    }

    public PageResponse<ResumeDto> myResumes(
        String ownerKey,
        String status,
        String templateKey,
        String keyword,
        int page,
        int pageSize
    ) {
        return resumeRepository.list(
            ownerKey,
            page,
            pageSize,
            normalize(status),
            normalize(templateKey),
            normalize(keyword)
        );
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record MeAggregateMetrics(
        long resourceCount,
        long favoriteCount,
        long roadmapCount,
        long noteCount,
        long resumeCount,
        long recentResourceCount,
        long publishedResourceCount,
        long draftNoteCount,
        long generatedResumeCount
    ) {
        private static MeAggregateMetrics empty() {
            return new MeAggregateMetrics(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
        }
    }
}
