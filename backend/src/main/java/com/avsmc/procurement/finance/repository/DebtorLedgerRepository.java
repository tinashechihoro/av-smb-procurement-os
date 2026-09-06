package com.avsmc.procurement.finance.repository;

import com.avsmc.procurement.finance.entity.DebtorLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface DebtorLedgerRepository extends JpaRepository<DebtorLedger, UUID> {
    List<DebtorLedger> findByCustomerIdOrderByEntryDateDesc(UUID customerId);
}
