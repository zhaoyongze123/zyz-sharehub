package com.sharehub.resource;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
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

    Page<ResourceEntity> findByStatusInAndVisibilityOrderByUpdatedAtDesc(List<String> statuses,
                                                                         String visibility,
                                                                         Pageable pageable);

    long countByStatusInAndVisibility(List<String> statuses, String visibility);

    @Query("""
        select r
        from ResourceEntity r
        where r.status in (:statuses)
          and (:keyword is null or lower(r.title) like lower(concat('%', :keyword, '%')) or lower(coalesce(r.summary, '')) like lower(concat('%', :keyword, '%')))
          and (:type is null or r.type = :type)
          and (:tag is null or lower(coalesce(r.tags, '')) like lower(concat('%', :tag, '%')))
          and (:visibility is null or r.visibility = :visibility)
        order by r.updatedAt desc
        """)
    Page<ResourceEntity> findVisibleByFiltersOrderByUpdatedAtDesc(
        @Param("statuses") List<String> statuses,
        @Param("keyword") String keyword,
        @Param("type") String type,
        @Param("tag") String tag,
        @Param("visibility") String visibility,
        Pageable pageable);

    @Query("""
        select count(r)
        from ResourceEntity r
        where r.status in (:statuses)
          and (:keyword is null or lower(r.title) like lower(concat('%', :keyword, '%')) or lower(coalesce(r.summary, '')) like lower(concat('%', :keyword, '%')))
          and (:type is null or r.type = :type)
          and (:tag is null or lower(coalesce(r.tags, '')) like lower(concat('%', :tag, '%')))
          and (:visibility is null or r.visibility = :visibility)
        """)
    long countByVisibleFilters(
        @Param("statuses") List<String> statuses,
        @Param("keyword") String keyword,
        @Param("type") String type,
        @Param("tag") String tag,
        @Param("visibility") String visibility
    );

    @Modifying
    @Query("update ResourceEntity r set r.status = :status where r.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Query("""
        select r
        from ResourceEntity r
        where r.status = 'PUBLISHED'
        order by r.updatedAt desc
        """)
    List<ResourceEntity> findTop6ByPublishedOrderByUpdatedAtDesc();

    @Query("""
        select r
        from ResourceEntity r
        where r.status = 'PUBLISHED'
          and r.id <> :id
        order by r.updatedAt desc
        """)
    List<ResourceEntity> findPublishedExcludingId(@Param("id") Long id);

    @Query("""
        select r
        from ResourceEntity r
        where r.id in :ids
        order by r.updatedAt desc
        """)
    List<ResourceEntity> findAllByIdInOrderByUpdatedAtDesc(@Param("ids") Collection<Long> ids);
}
