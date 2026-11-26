package com.swizima.filesapi.repository;

import com.swizima.filesapi.entity.GeneralFileAttachment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GeneralFileAttachmentRepository extends JpaRepository<GeneralFileAttachment, Long> {

    Page<GeneralFileAttachment> findByOrganisationIdAndEntityRefId(String organisationId, String entityRefId, Pageable pageable);
    Page<GeneralFileAttachment> findByOrganisationId(String organisationId, Pageable pageable);
    Page<GeneralFileAttachment> findByEntityRefId(String entityRefId, Pageable pageable);
    List<GeneralFileAttachment> findByEntityRefId(String entityRefId);
    Optional<GeneralFileAttachment> findByOrganisationIdAndEntityRefIdAndOriginalFilenameIgnoreCase(String organisationId, String entityRefId, String originalFilename);
    List<GeneralFileAttachment> findAllByOrganisationIdAndEntityRefIdAndOriginalFilenameIgnoreCase(String organisationId, String entityRefId, String originalFilename);
    List<GeneralFileAttachment> findAllByOrganisationIdAndEntityRefIdAndCategory(String organisationId, String entityRefId, String category);
    Optional<GeneralFileAttachment> findByFileUuid(String fileUuid);
    boolean existsByFileUuid(String fileUuid);
}
