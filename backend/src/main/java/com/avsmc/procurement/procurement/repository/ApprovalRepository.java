package com.avsmc.procurement.procurement.repository;

import com.avsmc.procurement.procurement.entity.Approval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApprovalRepository extends JpaRepository<Approval, UUID> {
    List<Approval> findByEntityTypeAndEntityId(String entityType, UUID entityId);
    List<Approval> findByOrganisationIdAndStatus(UUID organisationId, String status);
}
