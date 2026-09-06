package com.avsmc.procurement.workshop.repository;

import com.avsmc.procurement.workshop.entity.RepairStageHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface RepairStageHistoryRepository extends JpaRepository<RepairStageHistory, UUID> {
    List<RepairStageHistory> findByRepairJobIdOrderByCreatedAtDesc(UUID repairJobId);
}
