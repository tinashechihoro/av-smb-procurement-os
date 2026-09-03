package com.avsmc.procurement.finance.repository;

import com.avsmc.procurement.finance.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByOrganisationId(UUID organisationId);
    List<Payment> findByInvoiceId(UUID invoiceId);
}
