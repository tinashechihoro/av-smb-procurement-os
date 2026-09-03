package com.avsmc.procurement.workshop.repository;

import com.avsmc.procurement.workshop.entity.RepairJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface RepairJobRepository extends JpaRepository<RepairJob, UUID> {
    List<RepairJob> findByOrganisationId(UUID organisationId);
    List<RepairJob> findByVehicleId(UUID vehicleId);
    List<RepairJob> findByOrganisationIdAndStatus(UUID organisationId, String status);

    @Query("SELECT j FROM RepairJob j LEFT JOIN FETCH j.vehicle WHERE j.organisationId = :orgId")
    List<RepairJob> findByOrgWithVehicle(UUID orgId);
}
