package com.avsmc.procurement.inventory.repository;

import com.avsmc.procurement.inventory.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {
    List<InventoryItem> findByOrganisationId(UUID organisationId);
    List<InventoryItem> findByOrganisationIdAndStatus(UUID organisationId, String status);
}
