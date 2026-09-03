package com.avsmc.procurement.procurement.repository;

import com.avsmc.procurement.procurement.entity.Quotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuotationRepository extends JpaRepository<Quotation, UUID> {
    List<Quotation> findByOrganisationId(UUID organisationId);
    List<Quotation> findByRequisitionId(UUID requisitionId);
}
