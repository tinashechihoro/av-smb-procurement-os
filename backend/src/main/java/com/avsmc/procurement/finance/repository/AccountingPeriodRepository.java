package com.avsmc.procurement.finance.repository;

import com.avsmc.procurement.finance.entity.AccountingPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AccountingPeriodRepository extends JpaRepository<AccountingPeriod, UUID> {
    List<AccountingPeriod> findByOrganisationIdOrderByStartDateDesc(UUID organisationId);
}
