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

  public RoadmapDto create(RoadmapDto req) {
    RoadmapDto payload =
        new RoadmapDto(null, req.title(), req.description(), req.visibility(), req.status() == null ? "PUBLISHED" : req.status());
    return repository.save(payload);
  }

  public PageResponse<RoadmapDto> list(int page, int pageSize) {
    int safePage = Math.max(1, page);
    int safeSize = Math.max(1, pageSize);
    return repository.list(safePage, safeSize);
  }

  public RoadmapDetailResponse detail(Long id) {
    RoadmapDto roadmap = repository.findById(id).orElse(null);
    if (roadmap == null) {
      return null;
    }
    List<RoadmapNodeDto> flatNodes = repository.findNodes(id);
    List<RoadmapNodeTree> tree = buildTree(flatNodes);
    Map<String, Object> progress = repository.findProgress(id);
    return new RoadmapDetailResponse(roadmap, tree, progress == null ? Map.of() : progress);
  }

  public List<RoadmapNodeDto> addNode(Long id, RoadmapNodeDto req) {
    return repository.addNode(id, req);
  }

  public Map<String, Object> updateProgress(Long id, Map<String, Object> payload) {
    return repository.saveProgress(id, payload);
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
