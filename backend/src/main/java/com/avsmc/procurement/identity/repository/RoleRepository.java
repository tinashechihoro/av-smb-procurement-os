package com.avsmc.procurement.identity.repository;

import com.avsmc.procurement.identity.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    List<Role> findByOrganisationId(UUID organisationId);
    Optional<Role> findByOrganisationIdAndCode(UUID organisationId, String code);
}
