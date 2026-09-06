package com.avsmc.procurement.inventory.entity;

import com.avsmc.procurement.shared.entity.OrganisationScoped;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "locations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Location extends OrganisationScoped {
    @Column(nullable = false, length = 200) private String name;
    @Column(nullable = false, length = 30) private String code;
    @Column(name = "location_type", nullable = false, length = 20) @Builder.Default private String locationType = "BIN";
    @Column(name = "is_active", nullable = false) @Builder.Default private Boolean isActive = true;
}
