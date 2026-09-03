package com.avsmc.procurement.purchasing.repository;

import com.avsmc.procurement.purchasing.entity.SupplierPurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SupplierPurchaseOrderRepository extends JpaRepository<SupplierPurchaseOrder, UUID> {
    List<SupplierPurchaseOrder> findByOrganisationId(UUID organisationId);
    List<SupplierPurchaseOrder> findBySupplierId(UUID supplierId);
    List<SupplierPurchaseOrder> findByAvOrderId(UUID avOrderId);
}
