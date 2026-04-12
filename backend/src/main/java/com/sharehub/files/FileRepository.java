package com.sharehub.files;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileRepository extends JpaRepository<FileRecord, UUID> {
    Optional<FileRecord> findFirstByReferenceTypeAndReferenceIdAndCategoryOrderByCreatedAtDesc(
        String referenceType,
        String referenceId,
        FileCategory category
    );

    List<FileRecord> findByReferenceTypeAndCategoryAndReferenceIdInOrderByCreatedAtAsc(
        String referenceType,
        FileCategory category,
        Collection<String> referenceIds
    );
}
