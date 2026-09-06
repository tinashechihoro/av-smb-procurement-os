package com.avsmc.procurement.workshop.entity;

import com.avsmc.procurement.shared.entity.OrganisationScoped;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "insurers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Insurer extends OrganisationScoped {
    @Column(nullable = false, length = 200) private String name;
    @Column(name = "contact_person", length = 200) private String contactPerson;
    @Column(length = 255) private String email;
    @Column(length = 30) private String phone;
    @Column(name = "policy_prefix", length = 30) private String policyPrefix;
    @Column(name = "is_active", nullable = false) @Builder.Default private Boolean isActive = true;
}
