package com.avsmc.procurement.inventory.repository;

import com.avsmc.procurement.inventory.entity.DeliveryNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeliveryNoteRepository extends JpaRepository<DeliveryNote, UUID> {
    List<DeliveryNote> findByOrganisationId(UUID organisationId);
    List<DeliveryNote> findByAvOrderId(UUID avOrderId);
}
