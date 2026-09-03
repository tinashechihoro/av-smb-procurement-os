package com.avsmc.procurement.finance.repository;

import com.avsmc.procurement.finance.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    List<Invoice> findByOrganisationId(UUID organisationId);
    List<Invoice> findByOrganisationIdAndStatus(UUID organisationId, String status);
}
