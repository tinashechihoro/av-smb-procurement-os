package com.avsmc.procurement.workshop.controller;

import com.avsmc.procurement.workshop.dto.*;
import com.avsmc.procurement.workshop.service.WorkshopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class WorkshopController {

    private final WorkshopService workshopService;

    @GetMapping("/vehicles")
    public ResponseEntity<List<VehicleDto>> listVehicles() {
        return ResponseEntity.ok(workshopService.listVehicles());
    }

    @GetMapping("/vehicles/{id}")
    public ResponseEntity<VehicleDto> getVehicle(@PathVariable UUID id) {
        return ResponseEntity.ok(workshopService.getVehicle(id));
    }

    @PostMapping("/vehicles")
    public ResponseEntity<VehicleDto> createVehicle(@Valid @RequestBody CreateVehicleRequest request) {
        return ResponseEntity.ok(workshopService.createVehicle(request));
    }

    @GetMapping("/repair-jobs")
    public ResponseEntity<List<RepairJobDto>> listJobs() {
        return ResponseEntity.ok(workshopService.listJobs());
    }

    @GetMapping("/repair-jobs/{id}")
    public ResponseEntity<RepairJobDto> getJob(@PathVariable UUID id) {
        return ResponseEntity.ok(workshopService.getJob(id));
    }

    @PostMapping("/repair-jobs")
    public ResponseEntity<RepairJobDto> createJob(@Valid @RequestBody CreateRepairJobRequest request) {
        return ResponseEntity.ok(workshopService.createJob(request));
    }

    @PatchMapping("/repair-jobs/{id}/status")
    public ResponseEntity<RepairJobDto> updateJobStatus(@PathVariable UUID id,
                                                         @Valid @RequestBody UpdateJobStatusRequest request) {
        return ResponseEntity.ok(workshopService.updateJobStatus(id, request));
    }

    @PostMapping("/repair-jobs/{id}/damage-assessments")
    public ResponseEntity<DamageAssessmentDto> addDamageAssessment(@PathVariable UUID id,
                                                                    @Valid @RequestBody DamageAssessmentDto dto) {
        return ResponseEntity.ok(workshopService.addDamageAssessment(id, dto));
    }

    @PostMapping("/repair-jobs/{id}/operations")
    public ResponseEntity<RepairOperationDto> addOperation(@PathVariable UUID id,
                                                            @Valid @RequestBody RepairOperationDto dto) {
        return ResponseEntity.ok(workshopService.addOperation(id, dto));
    }
}
