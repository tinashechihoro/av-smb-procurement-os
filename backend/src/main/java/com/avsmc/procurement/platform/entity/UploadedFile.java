package com.avsmc.procurement.platform.entity;

import com.avsmc.procurement.shared.entity.OrganisationScoped;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "uploaded_files")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UploadedFile extends OrganisationScoped {

    @Column(name = "original_name", nullable = false, length = 300)
    private String originalName;

    @Column(name = "stored_name", nullable = false, length = 300)
    private String storedName;

    @Column(name = "storage_path", nullable = false, columnDefinition = "TEXT")
    private String storagePath;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(length = 64)
    private String checksum;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;
}
