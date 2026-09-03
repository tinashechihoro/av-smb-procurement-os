package com.avsmc.procurement.workshop.repository;

import com.avsmc.procurement.workshop.entity.RepairOperation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RepairOperationRepository extends JpaRepository<RepairOperation, UUID> {
    List<RepairOperation> findByRepairJobIdOrderBySequenceOrder(UUID repairJobId);
}
