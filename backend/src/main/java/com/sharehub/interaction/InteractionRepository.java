package com.sharehub.interaction;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InteractionRepository {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Path storage = Path.of("data/interaction.json");
    private final AtomicLong idGen = new AtomicLong(2000);
    private final Map<Long, CommentRecord> comments = new LinkedHashMap<>();
    private final Map<Long, ReportRecord> reports = new LinkedHashMap<>();
    private final Map<Long, Integer> favorites = new LinkedHashMap<>();
    private final Map<Long, Integer> likes = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        if (Files.exists(storage)) {
            try {
                Snapshot snapshot = mapper.readValue(storage.toFile(), Snapshot.class);
                snapshot.comments().forEach(comment -> comments.put(comment.id(), comment));
                snapshot.reports().forEach(report -> reports.put(report.id(), report));
                favorites.putAll(snapshot.favorites());
                likes.putAll(snapshot.likes());
                snapshot.comments().forEach(comment -> idGen.updateAndGet(curr -> Math.max(curr, comment.id())));
                snapshot.reports().forEach(report -> idGen.updateAndGet(curr -> Math.max(curr, report.id())));
            } catch (IOException ignored) {
            }
        }
    }

    public synchronized CommentRecord saveComment(Long resourceId, String content, Long parentId) {
        long id = idGen.incrementAndGet();
        CommentRecord record = new CommentRecord(id, resourceId, content, parentId, "OPEN");
        comments.put(id, record);
        persist();
        return record;
    }

    public synchronized ReportRecord saveReport(Long resourceId, String reason, String reporter) {
        long id = idGen.incrementAndGet();
        ReportRecord report = new ReportRecord(id, resourceId, reason, reporter, "OPEN");
        reports.put(id, report);
        persist();
        return report;
    }

    public synchronized int addFavorite(Long resourceId) {
        favorites.merge(resourceId, 1, Integer::sum);
        persist();
        return favorites.get(resourceId);
    }

    public synchronized int addLike(Long resourceId) {
        likes.merge(resourceId, 1, Integer::sum);
        persist();
        return likes.get(resourceId);
    }

    public synchronized List<ReportRecord> listReports() {
        return new ArrayList<>(reports.values());
    }

    public synchronized ReportRecord resolveReport(Long id) {
        ReportRecord current = reports.get(id);
        if (current == null) {
            return null;
        }
        ReportRecord updated = new ReportRecord(id, current.resourceId(), current.reason(), current.reporter(), "RESOLVED");
        reports.put(id, updated);
        persist();
        return updated;
    }

    private void persist() {
        try {
            Files.createDirectories(storage.getParent());
            Snapshot snapshot = new Snapshot(new ArrayList<>(comments.values()), new ArrayList<>(reports.values()), new LinkedHashMap<>(favorites), new LinkedHashMap<>(likes));
            mapper.writeValue(storage.toFile(), snapshot);
        } catch (IOException ignored) {
        }
    }

    private record Snapshot(List<CommentRecord> comments, List<ReportRecord> reports, Map<Long, Integer> favorites, Map<Long, Integer> likes) {}

    public record CommentRecord(Long id, Long resourceId, String content, Long parentId, String status) {}

    public record ReportRecord(Long id, Long resourceId, String reason, String reporter, String status) {}
}
