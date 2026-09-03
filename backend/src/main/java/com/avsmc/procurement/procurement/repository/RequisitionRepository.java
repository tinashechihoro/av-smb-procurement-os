package com.avsmc.procurement.procurement.repository;

import com.avsmc.procurement.procurement.entity.Requisition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RequisitionRepository extends JpaRepository<Requisition, UUID> {
    List<Requisition> findByOrganisationId(UUID organisationId);
    List<Requisition> findByOrganisationIdAndStatus(UUID organisationId, String status);
    List<Requisition> findByRepairJobId(UUID repairJobId);
}
