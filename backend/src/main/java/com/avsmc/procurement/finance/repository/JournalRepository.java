package com.avsmc.procurement.finance.repository;

import com.avsmc.procurement.finance.entity.Journal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JournalRepository extends JpaRepository<Journal, UUID> {
    List<Journal> findByOrganisationId(UUID organisationId);
    List<Journal> findByOrganisationIdAndStatus(UUID organisationId, String status);
}
