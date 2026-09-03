package com.avsmc.procurement.finance.repository;

import com.avsmc.procurement.finance.entity.CashbookEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CashbookEntryRepository extends JpaRepository<CashbookEntry, UUID> {
    List<CashbookEntry> findByOrganisationId(UUID organisationId);
    List<CashbookEntry> findByCashAccountId(UUID cashAccountId);
}
