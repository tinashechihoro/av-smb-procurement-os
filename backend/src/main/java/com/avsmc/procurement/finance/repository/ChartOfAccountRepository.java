package com.avsmc.procurement.finance.repository;

import com.avsmc.procurement.finance.entity.ChartOfAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChartOfAccountRepository extends JpaRepository<ChartOfAccount, UUID> {
    List<ChartOfAccount> findByOrganisationId(UUID organisationId);
    Optional<ChartOfAccount> findByOrganisationIdAndAccountCode(UUID organisationId, String accountCode);
}
