package com.avsmc.procurement.finance.repository;

import com.avsmc.procurement.finance.entity.CreditorLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CreditorLedgerRepository extends JpaRepository<CreditorLedger, UUID> {
    List<CreditorLedger> findBySupplierIdOrderByEntryDateDesc(UUID supplierId);
}
