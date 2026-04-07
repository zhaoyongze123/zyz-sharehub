package com.sharehub.me;

import com.sharehub.auth.UserProfileDto;
import com.sharehub.auth.UserProfileRepository;
import com.sharehub.note.NoteRepository;
import com.sharehub.resume.ResumeRepository;
import com.sharehub.roadmap.RoadmapJdbcRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class MeService {

    private final UserProfileRepository userProfileRepository;
    private final JdbcTemplate jdbcTemplate;
    private final RoadmapJdbcRepository roadmapJdbcRepository;
    private final NoteRepository noteRepository;
    private final ResumeRepository resumeRepository;

    public MeService(
        UserProfileRepository userProfileRepository,
        JdbcTemplate jdbcTemplate,
        RoadmapJdbcRepository roadmapJdbcRepository,
        NoteRepository noteRepository,
        ResumeRepository resumeRepository
    ) {
        this.userProfileRepository = userProfileRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.roadmapJdbcRepository = roadmapJdbcRepository;
        this.noteRepository = noteRepository;
        this.resumeRepository = resumeRepository;
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
}
