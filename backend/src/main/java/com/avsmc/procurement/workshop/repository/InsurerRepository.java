package com.avsmc.procurement.workshop.repository;

import com.avsmc.procurement.workshop.entity.Insurer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface InsurerRepository extends JpaRepository<Insurer, UUID> {
    List<Insurer> findByOrganisationId(UUID organisationId);
}
