package com.avsmc.procurement.finance.entity;

import com.avsmc.procurement.shared.entity.OrganisationScoped;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "accounting_periods")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AccountingPeriod extends OrganisationScoped {
    @Column(name = "period_name", nullable = false, length = 50) private String periodName;
    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Column(name = "end_date", nullable = false) private LocalDate endDate;
    @Column(nullable = false, length = 20) @Builder.Default private String status = "OPEN";
    @Column(name = "closed_by") private UUID closedBy;
    @Column(name = "closed_at") private Instant closedAt;
}
