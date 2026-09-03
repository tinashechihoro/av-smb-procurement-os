package com.avsmc.procurement.inventory.repository;

import com.avsmc.procurement.inventory.entity.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, UUID> {
    List<GoodsReceipt> findByOrganisationId(UUID organisationId);
}
