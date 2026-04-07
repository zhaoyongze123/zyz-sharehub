package com.sharehub.resource;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

import org.springframework.data.repository.query.Param;

public interface ResourceRepository extends JpaRepository<ResourceEntity, Long> {

    @Query("""
        select r
        from ResourceEntity r
        where r.ownerKey = :ownerKey
          and (:status is null or r.status = :status)
          and (:visibility is null or r.visibility = :visibility)
        order by r.updatedAt desc
        """)
    Page<ResourceEntity> findByOwnerAndFilters(
        @Param("ownerKey") String ownerKey,
        @Param("status") String status,
        @Param("visibility") String visibility,
        Pageable pageable
    );

    @Query("""
        select r
        from ResourceEntity r
        where (:status is null or r.status = :status)
          and (:visibility is null or r.visibility = :visibility)
        order by r.updatedAt desc
        """)
    Page<ResourceEntity> findByStatusAndVisibility(@Param("status") String status,
                                                   @Param("visibility") String visibility,
                                                   Pageable pageable);

    @Query("""
        select count(r)
        from ResourceEntity r
        where (:status is null or r.status = :status)
          and (:visibility is null or r.visibility = :visibility)
        """)
    long countByStatusAndVisibility(@Param("status") String status,
                                    @Param("visibility") String visibility);

    @Query("""
        select r
        from ResourceEntity r
        where r.status = 'PUBLISHED'
        order by r.updatedAt desc
        """)
    List<ResourceEntity> findTop6ByPublishedOrderByUpdatedAtDesc();
}
