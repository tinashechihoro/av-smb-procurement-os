package com.avsmc.procurement.platform.repository;

import com.avsmc.procurement.platform.entity.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, UUID> {
    List<UploadedFile> findByEntityTypeAndEntityId(String entityType, UUID entityId);
    List<UploadedFile> findByOrganisationId(UUID organisationId);
}
