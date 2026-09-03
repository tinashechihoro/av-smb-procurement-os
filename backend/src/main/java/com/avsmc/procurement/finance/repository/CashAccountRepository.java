package com.avsmc.procurement.finance.repository;

import com.avsmc.procurement.finance.entity.CashAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CashAccountRepository extends JpaRepository<CashAccount, UUID> {
    List<CashAccount> findByOrganisationId(UUID organisationId);
}
