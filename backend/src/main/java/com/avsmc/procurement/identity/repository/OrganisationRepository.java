package com.avsmc.procurement.identity.repository;

import com.avsmc.procurement.identity.entity.Organisation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganisationRepository extends JpaRepository<Organisation, UUID> {
    Optional<Organisation> findByCode(String code);
}
