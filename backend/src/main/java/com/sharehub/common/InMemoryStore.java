package com.sharehub.common;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InMemoryStore {
    private final AtomicLong id = new AtomicLong(1000);

    public final Map<Long, Object> resources = new ConcurrentHashMap<>();
    public final Map<Long, Object> roadmaps = new ConcurrentHashMap<>();
    public final Map<Long, Object> notes = new ConcurrentHashMap<>();
    public final Map<Long, Object> resumes = new ConcurrentHashMap<>();
    public final Map<Long, Object> reports = new ConcurrentHashMap<>();

    public long nextId() {
        return id.incrementAndGet();
    }
}
