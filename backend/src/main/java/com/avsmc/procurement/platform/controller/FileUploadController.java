package com.avsmc.procurement.platform.controller;

import com.avsmc.procurement.platform.entity.UploadedFile;
import com.avsmc.procurement.platform.repository.UploadedFileRepository;
import com.avsmc.procurement.security.SecurityUtils;
import com.avsmc.procurement.shared.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final UploadedFileRepository fileRepository;
    private final SecurityUtils securityUtils;

    @Value("${app.storage.local-path:./uploads}")
    private String storagePath;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/csv"
    );

    private static final long MAX_FILE_SIZE = 25 * 1024 * 1024;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "entityType", required = false) String entityType,
            @RequestParam(value = "entityId", required = false) UUID entityId) throws IOException {

        if (file.isEmpty()) throw new BusinessRuleException("File is empty");
        if (file.getSize() > MAX_FILE_SIZE) throw new BusinessRuleException("File exceeds 25MB limit");
        if (!ALLOWED_TYPES.contains(file.getContentType()))
            throw new BusinessRuleException("File type not allowed: " + file.getContentType());

        String checksum = computeChecksum(file.getBytes());
        String storedName = UUID.randomUUID() + "_" + sanitizeFilename(file.getOriginalFilename());
        String datePath = LocalDate.now().toString().replace('-', '/');
        Path dir = Paths.get(storagePath, securityUtils.currentOrgId().toString(), datePath);
        Files.createDirectories(dir);
        Path target = dir.resolve(storedName);
        file.transferTo(target.toFile());

        UploadedFile record = UploadedFile.builder()
                .originalName(file.getOriginalFilename())
                .storedName(storedName)
                .storagePath(target.toString())
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .checksum(checksum)
                .entityType(entityType)
                .entityId(entityId)
                .uploadedBy(securityUtils.currentUserId())
                .build();
        record.setOrganisationId(securityUtils.currentOrgId());

        fileRepository.save(record);

        return ResponseEntity.ok(Map.of(
                "id", record.getId(),
                "originalName", record.getOriginalName(),
                "fileSize", record.getFileSize(),
                "mimeType", record.getMimeType()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) throws IOException {
        UploadedFile file = fileRepository.findById(id)
                .orElseThrow(() -> new com.avsmc.procurement.shared.exception.ResourceNotFoundException("File", id));

        if (!securityUtils.currentOrgId().equals(file.getOrganisationId())) {
            throw new com.avsmc.procurement.shared.exception.ResourceNotFoundException("File", id);
        }

        byte[] data = Files.readAllBytes(Paths.get(file.getStoragePath()));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getOriginalName() + "\"")
                .contentType(MediaType.parseMediaType(file.getMimeType()))
                .body(data);
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<List<Map<String, Object>>> listByEntity(@PathVariable String entityType, @PathVariable UUID entityId) {
        List<Map<String, Object>> files = fileRepository
                .findByEntityTypeAndEntityIdAndOrganisationId(entityType, entityId, securityUtils.currentOrgId())
                .stream().map(f -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", f.getId());
                    m.put("originalName", f.getOriginalName());
                    m.put("fileSize", f.getFileSize());
                    m.put("mimeType", f.getMimeType());
                    m.put("createdAt", f.getCreatedAt());
                    return m;
                }).toList();
        return ResponseEntity.ok(files);
    }

    private String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String computeChecksum(byte[] data) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new IOException("Checksum failed", e); }
    }
}
