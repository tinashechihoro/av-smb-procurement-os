package com.avsmc.procurement.procurement.repository;

import com.avsmc.procurement.procurement.entity.OrderAmendment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface OrderAmendmentRepository extends JpaRepository<OrderAmendment, UUID> {
    List<OrderAmendment> findByOrderIdOrderByCreatedAtDesc(UUID orderId);
}
