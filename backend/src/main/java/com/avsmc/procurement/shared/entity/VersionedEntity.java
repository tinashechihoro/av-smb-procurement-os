package com.avsmc.procurement.shared.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class VersionedEntity extends BaseEntity {

    @Version
    @Column(name = "version")
    private Long version;
}
