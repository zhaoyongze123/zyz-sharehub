package com.sharehub.roadmap;

import com.sharehub.common.NotFoundException;
import com.sharehub.common.PageResponse;
import com.sharehub.files.FileCategory;
import com.sharehub.files.FileRecord;
import com.sharehub.files.FileRepository;
import com.sharehub.files.FileStorageService;
import com.sharehub.files.StoredFileDto;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class RoadmapService {

  private final RoadmapJdbcRepository repository;
  private final RoadmapEnrollmentRepository enrollmentRepository;
  private final FileRepository fileRepository;
  private final FileStorageService fileStorageService;

  public RoadmapService(
      RoadmapJdbcRepository repository,
      RoadmapEnrollmentRepository enrollmentRepository,
      FileRepository fileRepository,
      FileStorageService fileStorageService) {
    this.repository = repository;
    this.enrollmentRepository = enrollmentRepository;
    this.fileRepository = fileRepository;
    this.fileStorageService = fileStorageService;
  }

  public RoadmapDto create(String ownerKey, RoadmapDto req) {
    String normalizedStatus = normalizeStatus(req.status());
    RoadmapDto payload =
        new RoadmapDto(null, req.title(), req.description(), req.visibility(), normalizedStatus);
    return repository.save(ownerKey, payload);
  }

  public PageResponse<RoadmapDto> list(int page, int pageSize) {
    int safePage = Math.max(1, page);
    int safeSize = Math.max(1, pageSize);
    return repository.list(safePage, safeSize);
  }

  public RoadmapDetailResponse detail(Long id, String ownerKey) {
    RoadmapDto roadmap = repository.findById(id).orElse(null);
    if (roadmap == null) {
      return null;
    }
    List<RoadmapNodeDto> flatNodes = enrichNodes(repository.findNodes(id));
    List<RoadmapNodeTree> tree = buildTree(flatNodes);
    Map<String, Object> progress = ownerKey == null ? Map.of() : repository.findProgress(id, ownerKey);
    RoadmapEnrollmentDto enrollment = ownerKey == null
        ? null
        : enrollmentRepository.findByRoadmapIdAndUserKey(id, ownerKey).orElse(null);
    return new RoadmapDetailResponse(roadmap, tree, progress == null ? Map.of() : progress, enrollment);
  }

  public List<RoadmapNodeDto> addNode(String ownerKey, Long id, RoadmapNodeDto req) {
    return enrichNodes(repository.addNode(ownerKey, id, req));
  }

  public RoadmapNodeAttachmentDto uploadNodeAttachment(
      String ownerKey, Long roadmapId, Long nodeId, MultipartFile file) {
    if (!repository.existsOwnedNode(roadmapId, nodeId, ownerKey)) {
      throw new NotFoundException("ROADMAP_NOT_FOUND");
    }
    StoredFileDto stored =
        fileStorageService.storeMultipart(
            ownerKey,
            FileCategory.ROADMAP_NODE_ATTACHMENT,
            "ROADMAP_NODE",
            String.valueOf(nodeId),
            file);
    return new RoadmapNodeAttachmentDto(
        stored.id(),
        stored.filename(),
        stored.contentType(),
        stored.size(),
        stored.downloadUrl(),
        stored.createdAt());
  }

  public Map<String, Object> updateProgress(String ownerKey, Long id, Map<String, Object> payload) {
    return repository.saveProgress(ownerKey, id, payload);
  }

  public RoadmapEnrollmentDto enroll(String userKey, Long roadmapId) {
    repository.requireRoadmapExists(roadmapId);
    return enrollmentRepository.createOrActivate(roadmapId, userKey);
  }

  public RoadmapEnrollmentDto getEnrollment(String userKey, Long roadmapId) {
    repository.requireRoadmapExists(roadmapId);
    return enrollmentRepository.findByRoadmapIdAndUserKey(roadmapId, userKey).orElse(null);
  }

  public RoadmapEnrollmentDto pauseEnrollment(String userKey, Long roadmapId) {
    repository.requireRoadmapExists(roadmapId);
    return enrollmentRepository.pause(roadmapId, userKey);
  }

  public RoadmapEnrollmentDto resumeEnrollment(String userKey, Long roadmapId) {
    repository.requireRoadmapExists(roadmapId);
    return enrollmentRepository.resume(roadmapId, userKey);
  }

  public RoadmapEnrollmentDto completeEnrollment(String userKey, Long roadmapId) {
    repository.requireRoadmapExists(roadmapId);
    return enrollmentRepository.complete(roadmapId, userKey);
  }

  public void delete(String ownerKey, Long roadmapId) {
    repository.softDelete(ownerKey, roadmapId, Instant.now());
  }

  public RoadmapDto restore(String ownerKey, Long roadmapId) {
    return repository.restore(ownerKey, roadmapId);
  }

  private String normalizeStatus(String status) {
    if (status == null) {
      return "PUBLISHED";
    }
    String trimmed = status.trim();
    return trimmed.isEmpty() ? "PUBLISHED" : trimmed;
  }

  private List<RoadmapNodeTree> buildTree(List<RoadmapNodeDto> nodes) {
    Map<Long, RoadmapNodeTree> byId = new HashMap<>();
    List<RoadmapNodeTree> roots = new ArrayList<>();
    for (RoadmapNodeDto node : nodes) {
      byId.put(
          node.id(),
          new RoadmapNodeTree(
              node.id(),
              node.parentId(),
              node.title(),
              node.description(),
              node.orderNo(),
              node.resourceId(),
              node.noteId(),
              node.attachments(),
              new ArrayList<>()));
    }
    for (RoadmapNodeTree treeNode : byId.values()) {
      if (treeNode.parentId() == null) {
        roots.add(treeNode);
        continue;
      }
      RoadmapNodeTree parent = byId.get(treeNode.parentId());
      if (parent != null) {
        parent.children().add(treeNode);
        continue;
      }
      roots.add(treeNode);
    }
    return roots;
  }

  private List<RoadmapNodeDto> enrichNodes(List<RoadmapNodeDto> nodes) {
    Map<Long, List<RoadmapNodeAttachmentDto>> attachments = loadNodeAttachments(nodes);
    return nodes.stream()
        .map(
            node ->
                new RoadmapNodeDto(
                    node.id(),
                    node.parentId(),
                    node.title(),
                    node.description(),
                    node.orderNo(),
                    node.resourceId(),
                    node.noteId(),
                    attachments.getOrDefault(node.id(), List.of())))
        .toList();
  }

  private Map<Long, List<RoadmapNodeAttachmentDto>> loadNodeAttachments(List<RoadmapNodeDto> nodes) {
    List<String> referenceIds =
        nodes.stream().map(RoadmapNodeDto::id).filter(id -> id != null).map(String::valueOf).toList();
    if (referenceIds.isEmpty()) {
      return Map.of();
    }
    List<FileRecord> files =
        fileRepository.findByReferenceTypeAndCategoryAndReferenceIdInOrderByCreatedAtAsc(
            "ROADMAP_NODE", FileCategory.ROADMAP_NODE_ATTACHMENT, referenceIds);
    return files.stream()
        .collect(
            Collectors.groupingBy(
                file -> Long.valueOf(file.getReferenceId()),
                Collectors.mapping(
                    file ->
                        new RoadmapNodeAttachmentDto(
                            file.getId(),
                            file.getFilename(),
                            file.getContentType(),
                            file.getSize(),
                            "/api/files/" + file.getId(),
                            file.getCreatedAt()),
                    Collectors.toList())));
  }
}
