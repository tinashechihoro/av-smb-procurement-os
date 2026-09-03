package com.avsmc.procurement.finance.entity;

import com.avsmc.procurement.shared.entity.OrganisationScoped;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "journals")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Journal extends OrganisationScoped {

    @Column(name = "journal_number", nullable = false, unique = true, length = 30)
    private String journalNumber;

    @Column(name = "journal_type", nullable = false, length = 30)
    private String journalType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "source_type", length = 50)
    private String sourceType;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(name = "total_debit", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalDebit = BigDecimal.ZERO;

    @Column(name = "total_credit", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalCredit = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT";

    @Column(name = "posted_by")
    private UUID postedBy;

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @OneToMany(mappedBy = "journalId", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<JournalLine> lines = new ArrayList<>();
}
