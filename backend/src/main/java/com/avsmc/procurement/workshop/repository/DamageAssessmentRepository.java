package com.avsmc.procurement.workshop.repository;

import com.avsmc.procurement.workshop.entity.DamageAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DamageAssessmentRepository extends JpaRepository<DamageAssessment, UUID> {
    List<DamageAssessment> findByRepairJobId(UUID repairJobId);
}
