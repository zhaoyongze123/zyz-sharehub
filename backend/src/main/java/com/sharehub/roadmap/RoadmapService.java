package com.sharehub.roadmap;

import com.sharehub.common.PageResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RoadmapService {

  private final RoadmapJdbcRepository repository;

  public RoadmapService(RoadmapJdbcRepository repository) {
    this.repository = repository;
  }

  public RoadmapDto create(String ownerKey, RoadmapDto req) {
    RoadmapDto payload =
        new RoadmapDto(null, req.title(), req.description(), req.visibility(), normalizeStatus(req.status()));
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
    List<RoadmapNodeDto> flatNodes = repository.findNodes(id);
    List<RoadmapNodeTree> tree = buildTree(flatNodes);
    Map<String, Object> progress = ownerKey == null ? Map.of() : repository.findProgress(id, ownerKey);
    return new RoadmapDetailResponse(roadmap, tree, progress == null ? Map.of() : progress);
  }

  public List<RoadmapNodeDto> addNode(String ownerKey, Long id, RoadmapNodeDto req) {
    return repository.addNode(ownerKey, id, req);
  }

  public Map<String, Object> updateProgress(String ownerKey, Long id, Map<String, Object> payload) {
    return repository.saveProgress(ownerKey, id, payload);
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
          new RoadmapNodeTree(node.id(), node.parentId(), node.title(), node.orderNo(), node.resourceId(), node.noteId(), new ArrayList<>()));
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
}
