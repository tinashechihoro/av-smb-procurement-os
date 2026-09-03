package com.avsmc.procurement.workshop.service;

import com.avsmc.procurement.security.SecurityUtils;
import com.avsmc.procurement.shared.exception.ResourceNotFoundException;
import com.avsmc.procurement.shared.util.DocumentNumberGenerator;
import com.avsmc.procurement.workshop.dto.*;
import com.avsmc.procurement.workshop.entity.*;
import com.avsmc.procurement.workshop.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkshopService {

    private final VehicleRepository vehicleRepository;
    private final RepairJobRepository repairJobRepository;
    private final DamageAssessmentRepository damageAssessmentRepository;
    private final RepairOperationRepository repairOperationRepository;
    private final SecurityUtils securityUtils;
    private final DocumentNumberGenerator docNumberGenerator;

    @Transactional(readOnly = true)
    public List<VehicleDto> listVehicles() {
        return vehicleRepository.findByOrganisationId(securityUtils.currentOrgId())
                .stream().map(this::toVehicleDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VehicleDto getVehicle(UUID id) {
        return toVehicleDto(vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id)));
    }

    @Transactional
    public VehicleDto createVehicle(CreateVehicleRequest req) {
        Vehicle v = new Vehicle();
        v.setOrganisationId(securityUtils.currentOrgId());
        v.setRegistration(req.getRegistration());
        v.setMake(req.getMake());
        v.setModel(req.getModel());
        v.setYear(req.getYear());
        v.setColor(req.getColor());
        v.setVin(req.getVin());
        v.setEngineNumber(req.getEngineNumber());
        v.setMileage(req.getMileage());
        v.setFuelType(req.getFuelType());
        v.setVehicleType(req.getVehicleType());
        v.setCustomerId(req.getCustomerId());
        v.setInsurerId(req.getInsurerId());
        v.setNotes(req.getNotes());
        return toVehicleDto(vehicleRepository.save(v));
    }

    @Transactional(readOnly = true)
    public List<RepairJobDto> listJobs() {
        return repairJobRepository.findByOrgWithVehicle(securityUtils.currentOrgId())
                .stream().map(this::toJobDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RepairJobDto getJob(UUID id) {
        RepairJob job = repairJobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RepairJob", id));
        RepairJobDto dto = toJobDto(job);
        dto.setDamageAssessments(damageAssessmentRepository.findByRepairJobId(id)
                .stream().map(this::toDamageDto).collect(Collectors.toList()));
        dto.setOperations(repairOperationRepository.findByRepairJobIdOrderBySequenceOrder(id)
                .stream().map(this::toOperationDto).collect(Collectors.toList()));
        return dto;
    }

    @Transactional
    public RepairJobDto createJob(CreateRepairJobRequest req) {
        vehicleRepository.findById(req.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", req.getVehicleId()));
        RepairJob j = new RepairJob();
        j.setOrganisationId(securityUtils.currentOrgId());
        j.setVehicleId(req.getVehicleId());
        j.setJobNumber(docNumberGenerator.nextNumber(securityUtils.currentOrgId(), "REPAIR_JOB"));
        j.setTitle(req.getTitle());
        j.setDescription(req.getDescription());
        j.setJobType(req.getJobType() != null ? req.getJobType() : "COLLISION");
        j.setPriority(req.getPriority() != null ? req.getPriority() : "NORMAL");
        j.setAssignedTechnician(req.getAssignedTechnician());
        j.setBayNumber(req.getBayNumber());
        j.setBookedHours(req.getBookedHours());
        j.setLabourRate(req.getLabourRate());
        j.setInsurerClaimNumber(req.getInsurerClaimNumber());
        j.setExcessAmount(req.getExcessAmount());
        j.setNotes(req.getNotes());
        return toJobDto(repairJobRepository.save(j));
    }

    @Transactional
    public RepairJobDto updateJobStatus(UUID id, UpdateJobStatusRequest req) {
        RepairJob j = repairJobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RepairJob", id));
        j.setStatus(req.getStatus());
        if ("COMPLETED".equals(req.getStatus())) { j.setCompletedAt(Instant.now()); j.setRepairStage("COMPLETED"); }
        else if ("IN_REPAIR".equals(req.getStatus())) { if (j.getStartedAt() == null) j.setStartedAt(Instant.now()); j.setRepairStage("IN_REPAIR"); }
        return toJobDto(repairJobRepository.save(j));
    }

    @Transactional
    public DamageAssessmentDto addDamageAssessment(UUID jobId, DamageAssessmentDto dto) {
        repairJobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("RepairJob", jobId));
        DamageAssessment d = new DamageAssessment();
        d.setRepairJobId(jobId);
        d.setAssessedBy(securityUtils.currentUserId());
        d.setZone(dto.getZone());
        d.setSeverity(dto.getSeverity());
        d.setDescription(dto.getDescription());
        d.setPartsAffected(dto.getPartsAffected());
        d.setRepairMethod(dto.getRepairMethod());
        d.setEstimatedCost(dto.getEstimatedCost());
        return toDamageDto(damageAssessmentRepository.save(d));
    }

    @Transactional
    public RepairOperationDto addOperation(UUID jobId, RepairOperationDto dto) {
        repairJobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("RepairJob", jobId));
        RepairOperation o = new RepairOperation();
        o.setRepairJobId(jobId);
        o.setOperationName(dto.getOperationName());
        o.setOperationType(dto.getOperationType());
        o.setAssignedTo(dto.getAssignedTo());
        o.setBookedHours(dto.getBookedHours());
        o.setSequenceOrder(dto.getSequenceOrder() != null ? dto.getSequenceOrder() : 0);
        return toOperationDto(repairOperationRepository.save(o));
    }

    private VehicleDto toVehicleDto(Vehicle v) {
        return VehicleDto.builder().id(v.getId()).organisationId(v.getOrganisationId())
                .customerId(v.getCustomerId()).insurerId(v.getInsurerId())
                .registration(v.getRegistration()).vin(v.getVin())
                .make(v.getMake()).model(v.getModel()).year(v.getYear())
                .color(v.getColor()).engineNumber(v.getEngineNumber())
                .mileage(v.getMileage()).fuelType(v.getFuelType())
                .vehicleType(v.getVehicleType()).status(v.getStatus())
                .notes(v.getNotes()).build();
    }

    private RepairJobDto toJobDto(RepairJob j) {
        return RepairJobDto.builder().id(j.getId()).organisationId(j.getOrganisationId())
                .vehicleId(j.getVehicleId()).jobNumber(j.getJobNumber())
                .title(j.getTitle()).description(j.getDescription())
                .jobType(j.getJobType()).status(j.getStatus())
                .repairStage(j.getRepairStage()).priority(j.getPriority())
                .assignedTechnician(j.getAssignedTechnician()).bayNumber(j.getBayNumber())
                .bookedHours(j.getBookedHours()).actualHours(j.getActualHours())
                .labourRate(j.getLabourRate()).insurerClaimNumber(j.getInsurerClaimNumber())
                .insurerAuthorised(j.getInsurerAuthorised())
                .excessAmount(j.getExcessAmount()).supplementAmount(j.getSupplementAmount())
                .estimatedTotal(j.getEstimatedTotal()).actualTotal(j.getActualTotal())
                .startedAt(j.getStartedAt()).completedAt(j.getCompletedAt())
                .notes(j.getNotes()).build();
    }

    private DamageAssessmentDto toDamageDto(DamageAssessment d) {
        return DamageAssessmentDto.builder().id(d.getId()).repairJobId(d.getRepairJobId())
                .assessedBy(d.getAssessedBy()).zone(d.getZone()).severity(d.getSeverity())
                .description(d.getDescription()).partsAffected(d.getPartsAffected())
                .repairMethod(d.getRepairMethod()).estimatedCost(d.getEstimatedCost()).build();
    }

    private RepairOperationDto toOperationDto(RepairOperation o) {
        return RepairOperationDto.builder().id(o.getId()).repairJobId(o.getRepairJobId())
                .operationName(o.getOperationName()).operationType(o.getOperationType())
                .assignedTo(o.getAssignedTo()).bookedHours(o.getBookedHours())
                .actualHours(o.getActualHours()).sequenceOrder(o.getSequenceOrder())
                .status(o.getStatus()).notes(o.getNotes())
                .startedAt(o.getStartedAt()).completedAt(o.getCompletedAt()).build();
    }
}
