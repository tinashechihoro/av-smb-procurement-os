package com.avsmc.procurement.shared.util;

import com.avsmc.procurement.shared.exception.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DocumentNumberGenerator {

    private final EntityManager entityManager;

    public String nextNumber(UUID organisationId, String documentType) {
        Object[] result = (Object[]) entityManager
                .createNativeQuery(
                        "UPDATE document_sequences SET current_number = current_number + 1, updated_at = now() " +
                        "WHERE organisation_id = :orgId AND document_type = :type " +
                        "RETURNING prefix, current_number, padding")
                .setParameter("orgId", organisationId)
                .setParameter("type", documentType)
                .getSingleResult();

        String prefix = (String) result[0];
        long number = ((Number) result[1]).longValue();
        int padding = ((Number) result[2]).intValue();

        return prefix + "-" + String.format("%0" + padding + "d", number);
    }
}
