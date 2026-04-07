package com.sharehub.resource;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "resources")
public class ResourceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String type;

    @Column(columnDefinition = "TEXT")
    private String summary;

    private String tags;

    private String externalUrl;

    private String objectKey;

    @Column(nullable = false)
    private String ownerKey = "local-dev-user";

    private String visibility;

    private String status;

    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getTags() {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.asList(tags.split(","));
    }

    public void setTags(List<String> tags) {
        this.tags = tags == null ? null : String.join(",", tags);
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getOwnerKey() {
        return ownerKey;
    }

    public void setOwnerKey(String ownerKey) {
        this.ownerKey = ownerKey;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @PreUpdate
    public void touchUpdated() {
        this.updatedAt = Instant.now();
    }

    public ResourceDto toDto() {
        return toDto(null, 0L, 0L, 0L);
    }

    public ResourceDto toDto(String author, long likes, long favorites, long downloadCount) {
        return new ResourceDto(
            id,
            title,
            type,
            type,
            summary,
            getTags(),
            externalUrl,
            objectKey,
            visibility,
            status,
            updatedAt,
            author,
            likes,
            favorites,
            downloadCount
        );
    }
}
