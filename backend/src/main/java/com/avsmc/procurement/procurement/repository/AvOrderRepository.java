package com.avsmc.procurement.procurement.repository;

import com.avsmc.procurement.procurement.entity.AvOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AvOrderRepository extends JpaRepository<AvOrder, UUID> {
    List<AvOrder> findByOrganisationId(UUID organisationId);
    List<AvOrder> findByQuotationId(UUID quotationId);
}
