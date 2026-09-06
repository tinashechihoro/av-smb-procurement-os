package com.avsmc.procurement.procurement.repository;

import com.avsmc.procurement.procurement.entity.ClarificationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ClarificationMessageRepository extends JpaRepository<ClarificationMessage, UUID> {
    List<ClarificationMessage> findByThreadIdOrderByCreatedAtAsc(UUID threadId);
}
