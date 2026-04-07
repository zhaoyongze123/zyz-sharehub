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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class MeService {

    private static final String DEFAULT_OWNER_KEY = "local-dev-user";

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
        UserProfileDto profile = userProfileRepository.upsert(ownerKey, ownerKey, null);
        Long resourceCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM resources WHERE owner_key = ?",
            Long.class,
            ownerKey
        );
        Long favoriteCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM favorites WHERE user_key = ?",
            Long.class,
            ownerKey
        );
        return new MeDto(
            profile,
            resourceCount == null ? 0L : resourceCount,
            favoriteCount == null ? 0L : favoriteCount,
            roadmapJdbcRepository.countByOwner(ownerKey),
            noteRepository.countByOwner(ownerKey),
            resumeRepository.countByOwner(ownerKey)
        );
    }

    public PageResponse<ResourceDto> myResources(String status, String visibility, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, pageSize);
        Page<ResourceEntity> pageResult = resourceRepository.findByOwnerAndFilters(
            DEFAULT_OWNER_KEY,
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

    public PageResponse<RoadmapWorkbenchDto> myRoadmaps(String status, int page, int pageSize) {
        return roadmapJdbcRepository.listWorkbenchByOwner(DEFAULT_OWNER_KEY, status, page, pageSize);
    }

    public PageResponse<ResourceDto> myFavorites(int page, int pageSize) {
        return interactionRepository.listFavoriteResources(DEFAULT_OWNER_KEY, page, pageSize);
    }

    public PageResponse<NoteDto> myNotes(String status, int page, int pageSize) {
        return noteRepository.listByOwner(DEFAULT_OWNER_KEY, status, page, pageSize);
    }

    public PageResponse<ResumeDto> myResumes(String status, int page, int pageSize) {
        return resumeRepository.list(DEFAULT_OWNER_KEY, page, pageSize, status);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
