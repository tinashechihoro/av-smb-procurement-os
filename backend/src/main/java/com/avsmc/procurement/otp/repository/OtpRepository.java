package com.avsmc.procurement.otp.repository;

import com.avsmc.procurement.otp.entity.OtpCode;
import com.avsmc.procurement.otp.entity.OtpCode.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpRepository extends JpaRepository<OtpCode, UUID> {

    Optional<OtpCode> findFirstByUserIdAndPurposeAndIsUsedFalseOrderByCreatedAtDesc(UUID userId, OtpPurpose purpose);

    List<OtpCode> findByUserIdAndPurposeAndIsUsedFalse(UUID userId, OtpPurpose purpose);

    @Modifying
    @Query("DELETE FROM OtpCode o WHERE o.expiresAt < :cutoff")
    int deleteExpiredBefore(Instant cutoff);

    @Modifying
    @Query("UPDATE OtpCode o SET o.isUsed = true, o.verifiedAt = :verifiedAt WHERE o.id = :id")
    void markAsVerified(UUID id, Instant verifiedAt);

    @Modifying
    @Query("UPDATE OtpCode o SET o.failedAttempts = o.failedAttempts + 1 WHERE o.id = :id")
    void incrementFailedAttempts(UUID id);
}
