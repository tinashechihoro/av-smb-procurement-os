package com.avsmc.procurement.inventory.repository;

import com.avsmc.procurement.inventory.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {
    List<StockMovement> findByInventoryItemIdOrderByCreatedAtDesc(UUID inventoryItemId);
    List<StockMovement> findByOrganisationIdOrderByCreatedAtDesc(UUID organisationId);
}
