package com.avsmc.procurement.procurement.repository;

import com.avsmc.procurement.procurement.entity.QuotationVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface QuotationVersionRepository extends JpaRepository<QuotationVersion, UUID> {
    List<QuotationVersion> findByQuotationIdOrderByVersionNumberDesc(UUID quotationId);
}
