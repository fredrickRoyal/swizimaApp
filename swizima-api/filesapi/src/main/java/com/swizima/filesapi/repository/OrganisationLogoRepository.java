package com.swizima.filesapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

import com.swizima.filesapi.entity.OrganisationLogoFile;

@Repository
public interface OrganisationLogoRepository extends JpaRepository<OrganisationLogoFile, Long> {
    Optional<OrganisationLogoFile> findByOrganisationId(String organisationId);
    Optional<OrganisationLogoFile> findByFileUuid(String fileUuid);
    boolean existsByFileUuid(String fileUuid);
}
