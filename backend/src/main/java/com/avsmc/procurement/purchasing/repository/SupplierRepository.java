package com.avsmc.procurement.purchasing.repository;

import com.avsmc.procurement.purchasing.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {
    List<Supplier> findByOrganisationId(UUID organisationId);
    List<Supplier> findByOrganisationIdAndIsActive(UUID organisationId, Boolean isActive);
}
