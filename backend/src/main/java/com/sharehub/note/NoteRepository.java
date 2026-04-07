package com.sharehub.note;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class NoteRepository {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Path storage = Path.of("data/notes.json");
    private final AtomicLong idGenerator = new AtomicLong(1000);
    private final Map<Long, NoteDto> store = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        if (Files.exists(storage)) {
            try {
                List<NoteDto> list = mapper.readValue(storage.toFile(), new TypeReference<>() {});
                for (NoteDto note : list) {
                    store.put(note.id(), note);
                    idGenerator.updateAndGet(current -> Math.max(current, note.id()));
                }
            } catch (IOException e) {
                // ignore and start empty
            }
        }
    }

    public synchronized NoteDto save(NoteDto note) {
        long next = note.id() != null ? note.id() : idGenerator.incrementAndGet();
        NoteDto saved = new NoteDto(next, note.title(), note.contentMd(), note.visibility(), note.status());
        store.put(next, saved);
        persist();
        return saved;
    }

    public synchronized NoteDto upsert(Long id, NoteDto note) {
        long effective = id != null ? id : idGenerator.incrementAndGet();
        NoteDto saved = new NoteDto(effective, note.title(), note.contentMd(), note.visibility(), note.status());
        store.put(effective, saved);
        persist();
        return saved;
    }

    public synchronized boolean delete(Long id) {
        if (store.remove(id) != null) {
            persist();
            return true;
        }
        return false;
    }

    public synchronized NoteDto find(Long id) {
        return store.get(id);
    }

    public synchronized NotePage list(int page, int size) {
        List<NoteDto> sorted = store.values().stream()
            .sorted((a, b) -> Long.compare(b.id(), a.id()))
            .collect(Collectors.toList());
        int from = Math.min((page - 1) * size, sorted.size());
        int to = Math.min(from + size, sorted.size());
        List<NoteDto> slice = new ArrayList<>(sorted.subList(from, to));
        return new NotePage(slice, page, size, sorted.size());
    }

    private void persist() {
        try {
            Files.createDirectories(storage.getParent());
            mapper.writerFor(new TypeReference<List<NoteDto>>() {})
                .writeValue(storage.toFile(), new ArrayList<>(store.values()));
        } catch (IOException e) {
            // ignore persistence failures for now
        }
    }

    public record NotePage(List<NoteDto> items, int page, int size, int total) {}
}
