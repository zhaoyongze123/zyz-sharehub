package com.sharehub.resource;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ResourceRepository extends JpaRepository<ResourceEntity, Long> {
    Page<ResourceEntity> findByOwnerKeyOrderByIdDesc(String ownerKey, Pageable pageable);

    @Query("""
        select r
        from ResourceEntity r
        where r.status = 'PUBLISHED'
        order by r.updatedAt desc
        """)
    List<ResourceEntity> findTop6ByPublishedOrderByUpdatedAtDesc();
}
