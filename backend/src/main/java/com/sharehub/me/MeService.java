package com.sharehub.me;

import com.sharehub.auth.UserProfileDto;
import com.sharehub.auth.UserProfileRepository;
import com.sharehub.common.PageResponse;
import com.sharehub.interaction.InteractionRepository;
import com.sharehub.note.NoteDto;
import com.sharehub.note.NoteRepository;
import com.sharehub.note.RelatedNoteDto;
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
import org.springframework.stereotype.Service;

@Service
public class MeService {
    private static final Duration RECENT_RESOURCE_WINDOW = Duration.ofDays(7);
    private static final String PUBLISHED_STATUS = "PUBLISHED";
    private static final String DRAFT_STATUS = "DRAFT";
    private static final String GENERATED_STATUS = "GENERATED";

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
        UserProfileDto profile = userProfileRepository.upsert(ownerKey, null, null);
        Long resourceCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM resources WHERE owner_key = ? AND deleted_at IS NULL",
            Long.class,
            ownerKey
        );
        long safeResourceCount = resourceCount == null ? 0L : resourceCount;
        long safeFavoriteCount = interactionRepository.countFavoritedPublishedNotesByOwner(ownerKey);
        long recentResourceCount = countResourcesCreatedWithin(ownerKey, RECENT_RESOURCE_WINDOW);
        long publishedResourceCount = countResourcesByStatus(ownerKey, PUBLISHED_STATUS);
        long draftNoteCount = countNotesByStatus(ownerKey, DRAFT_STATUS);
        long generatedResumeCount = countResumesByStatus(ownerKey, GENERATED_STATUS);
        return new MeDto(
            profile,
            safeResourceCount,
            safeFavoriteCount,
            roadmapJdbcRepository.countByOwner(ownerKey),
            noteRepository.countByOwner(ownerKey),
            resumeRepository.countByOwner(ownerKey),
            recentResourceCount,
            publishedResourceCount,
            draftNoteCount,
            generatedResumeCount
        );
    }

    public UserProfileDto updateProfile(String ownerKey, String displayName, String bio) {
        return userProfileRepository.updateProfile(ownerKey, displayName, bio);
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

    public PageResponse<RoadmapWorkbenchDto> myEnrolledRoadmaps(String ownerKey, String status, int page, int pageSize) {
        return roadmapJdbcRepository.listWorkbenchByEnrollment(ownerKey, status, page, pageSize);
    }

    public PageResponse<RoadmapWorkbenchDto> myAuthoredRoadmaps(String ownerKey, String status, int page, int pageSize) {
        return roadmapJdbcRepository.listWorkbenchByOwner(ownerKey, status, page, pageSize);
    }

    public PageResponse<ResourceDto> myFavorites(String ownerKey, int page, int pageSize) {
        return interactionRepository.listFavoriteResources(ownerKey, page, pageSize);
    }

    public PageResponse<RelatedNoteDto> myFavoriteNotes(String ownerKey, int page, int pageSize) {
        return interactionRepository.listFavoriteNotes(ownerKey, page, pageSize);
    }

    public PageResponse<NoteDto> myNotes(String ownerKey, String status, int page, int pageSize) {
        return noteRepository.listByOwner(ownerKey, status, page, pageSize);
    }

    public PageResponse<RelatedNoteDto> myNoteHistory(String ownerKey, int page, int pageSize) {
        return noteRepository.listViewHistory(ownerKey, page, pageSize);
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

    private long countResourcesByStatus(String ownerKey, String status) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM resources WHERE owner_key = ? AND status = ? AND deleted_at IS NULL",
            Long.class,
            ownerKey,
            status
        );
        return count == null ? 0L : count;
    }

    private long countNotesByStatus(String ownerKey, String status) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM notes WHERE owner_key = ? AND status = ? AND deleted_at IS NULL",
            Long.class,
            ownerKey,
            status
        );
        return count == null ? 0L : count;
    }

    private long countResumesByStatus(String ownerKey, String status) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM resumes WHERE owner_key = ? AND status = ?",
            Long.class,
            ownerKey,
            status
        );
        return count == null ? 0L : count;
    }

    private long countResourcesCreatedWithin(String ownerKey, Duration window) {
        Instant threshold = Instant.now().minus(window);
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM resources WHERE owner_key = ? AND deleted_at IS NULL AND created_at >= ?",
            Long.class,
            ownerKey,
            Timestamp.from(threshold)
        );
        return count == null ? 0L : count;
    }
}
