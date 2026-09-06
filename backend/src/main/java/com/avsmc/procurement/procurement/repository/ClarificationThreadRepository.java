package com.avsmc.procurement.procurement.repository;

import com.avsmc.procurement.procurement.entity.ClarificationThread;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ClarificationThreadRepository extends JpaRepository<ClarificationThread, UUID> {
    List<ClarificationThread> findByRequisitionId(UUID requisitionId);
}
