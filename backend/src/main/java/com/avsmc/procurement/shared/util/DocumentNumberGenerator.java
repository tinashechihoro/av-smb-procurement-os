package com.avsmc.procurement.shared.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DocumentNumberGenerator {

    private final EntityManager entityManager;

    public String nextNumber(UUID organisationId, String documentType) {
        Object[] result;
        try {
            result = fetch(organisationId, documentType);
        } catch (NoResultException e) {
            // Sequence row missing (e.g. a document type never seeded for this
            // organisation): create it with a derived prefix and retry.
            entityManager.createNativeQuery(
                            "INSERT INTO document_sequences (organisation_id, document_type, prefix, current_number, padding) " +
                            "VALUES (:orgId, :type, :prefix, 0, 5) ON CONFLICT DO NOTHING")
                    .setParameter("orgId", organisationId)
                    .setParameter("type", documentType)
                    .setParameter("prefix", derivePrefix(documentType))
                    .executeUpdate();
            result = fetch(organisationId, documentType);
        }

        String prefix = (String) result[0];
        long number = ((Number) result[1]).longValue();
        int padding = ((Number) result[2]).intValue();

        return prefix + "-" + String.format("%0" + padding + "d", number);
    }

    private Object[] fetch(UUID organisationId, String documentType) {
        return (Object[]) entityManager
                .createNativeQuery(
                        "UPDATE document_sequences SET current_number = current_number + 1, updated_at = now() " +
                        "WHERE organisation_id = :orgId AND document_type = :type " +
                        "RETURNING prefix, current_number, padding")
                .setParameter("orgId", organisationId)
                .setParameter("type", documentType)
                .getSingleResult();
    }

    private String derivePrefix(String documentType) {
        String[] words = documentType.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0)));
        }
        return sb.length() >= 2 ? sb.toString() : sb + "X";
    }
}
