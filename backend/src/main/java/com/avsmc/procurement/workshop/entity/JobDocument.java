package com.avsmc.procurement.workshop.entity;

import com.avsmc.procurement.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "job_documents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JobDocument extends BaseEntity {

    @Column(name = "repair_job_id", nullable = false)
    private UUID repairJobId;

    @Column(name = "document_type", nullable = false, length = 30)
    private String documentType;

    @Column(name = "file_name", nullable = false, length = 300)
    private String fileName;

    @Column(name = "file_path", nullable = false, columnDefinition = "TEXT")
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;
}
