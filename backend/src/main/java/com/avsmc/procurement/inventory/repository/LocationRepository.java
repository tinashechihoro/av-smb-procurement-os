package com.avsmc.procurement.inventory.repository;

import com.avsmc.procurement.inventory.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LocationRepository extends JpaRepository<Location, UUID> {
    List<Location> findByOrganisationId(UUID organisationId);
}
