package com.avsmc.procurement.shared.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
public abstract class OrganisationScoped extends BaseEntity {

    @Column(name = "organisation_id", nullable = false, updatable = false)
    private UUID organisationId;
}
