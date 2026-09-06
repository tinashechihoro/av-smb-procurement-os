package com.avsmc.procurement.workshop.repository;

import com.avsmc.procurement.workshop.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    List<Customer> findByOrganisationId(UUID organisationId);
    List<Customer> findByOrganisationIdAndIsActive(UUID organisationId, Boolean isActive);
}
