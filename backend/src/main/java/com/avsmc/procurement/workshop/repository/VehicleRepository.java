package com.avsmc.procurement.workshop.repository;

import com.avsmc.procurement.workshop.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    List<Vehicle> findByOrganisationId(UUID organisationId);

    @Query("SELECT v FROM Vehicle v WHERE v.organisationId = :orgId AND " +
           "(LOWER(v.registration) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(v.make) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(v.model) LIKE LOWER(CONCAT('%',:q,'%')))")
    List<Vehicle> search(UUID orgId, String q);
}
