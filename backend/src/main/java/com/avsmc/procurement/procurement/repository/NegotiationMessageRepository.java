package com.avsmc.procurement.procurement.repository;

import com.avsmc.procurement.procurement.entity.NegotiationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface NegotiationMessageRepository extends JpaRepository<NegotiationMessage, UUID> {
    List<NegotiationMessage> findByQuotationIdOrderByCreatedAtAsc(UUID quotationId);
}
