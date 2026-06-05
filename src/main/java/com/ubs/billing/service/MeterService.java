package com.ubs.billing.service;

import com.ubs.billing.dto.mapper.MeterMapper;
import com.ubs.billing.dto.request.CreateMeterRequest;
import com.ubs.billing.dto.request.UpdateMeterRequest;
import com.ubs.billing.dto.response.MeterResponse;
import com.ubs.billing.dto.response.PageResponse;
import com.ubs.billing.entity.AuditAction;
import com.ubs.billing.entity.Customer;
import com.ubs.billing.entity.Meter;
import com.ubs.billing.entity.MeterStatus;
import com.ubs.billing.entity.MeterType;
import com.ubs.billing.exception.ConflictException;
import com.ubs.billing.exception.ResourceNotFoundException;
import com.ubs.billing.repository.CustomerRepository;
import com.ubs.billing.repository.MeterRepository;
import com.ubs.billing.util.AuditEntityNames;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class MeterService {

    private final MeterRepository meterRepository;
    private final CustomerRepository customerRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public MeterResponse createMeter(CreateMeterRequest request) {
        String meterNumber = request.getMeterNumber().trim();
        validateUniqueMeterNumber(meterNumber, null);

        Customer customer = findCustomerOrThrow(request.getCustomerId());

        Meter meter = Meter.builder()
                .meterNumber(meterNumber)
                .meterType(request.getMeterType())
                .installationDate(request.getInstallationDate())
                .status(request.getStatus())
                .customer(customer)
                .build();

        Meter savedMeter = meterRepository.save(meter);
        auditLogService.log(AuditAction.CREATE, AuditEntityNames.METER, savedMeter.getId());
        return MeterMapper.toResponse(findMeterWithCustomerOrThrow(savedMeter.getId()));
    }

    @Transactional
    public MeterResponse updateMeter(Long id, UpdateMeterRequest request) {
        Meter meter = findMeterWithCustomerOrThrow(id);
        String meterNumber = request.getMeterNumber().trim();

        validateUniqueMeterNumber(meterNumber, id);

        Customer customer = findCustomerOrThrow(request.getCustomerId());

        meter.setMeterNumber(meterNumber);
        meter.setMeterType(request.getMeterType());
        meter.setInstallationDate(request.getInstallationDate());
        meter.setStatus(request.getStatus());
        meter.setCustomer(customer);

        Meter updatedMeter = meterRepository.save(meter);
        auditLogService.log(AuditAction.UPDATE, AuditEntityNames.METER, updatedMeter.getId());
        return MeterMapper.toResponse(updatedMeter);
    }

    @Transactional
    public void deleteMeter(Long id) {
        Meter meter = findMeterOrThrow(id);
        auditLogService.log(AuditAction.DELETE, AuditEntityNames.METER, meter.getId());
        meterRepository.delete(meter);
    }

    @Transactional(readOnly = true)
    public MeterResponse getMeterById(Long id) {
        return MeterMapper.toResponse(findMeterWithCustomerOrThrow(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<MeterResponse> getAllMeters(
            String meterNumber,
            MeterType meterType,
            MeterStatus status,
            Long customerId,
            Pageable pageable) {

        Page<MeterResponse> page = meterRepository
                .searchMeters(
                        normalizeSearchParam(meterNumber),
                        meterType,
                        status,
                        customerId,
                        pageable)
                .map(MeterMapper::toResponse);

        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<MeterResponse> getCustomerMeters(
            Long customerId,
            String meterNumber,
            MeterType meterType,
            MeterStatus status,
            Pageable pageable) {

        findCustomerOrThrow(customerId);

        Page<MeterResponse> page = meterRepository
                .findByCustomerId(
                        customerId,
                        normalizeSearchParam(meterNumber),
                        meterType,
                        status,
                        pageable)
                .map(MeterMapper::toResponse);

        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public Meter findActiveMeterOrThrow(Long id) {
        Meter meter = findMeterWithCustomerOrThrow(id);
        if (!meter.isActive()) {
            if (meter.getStatus() == MeterStatus.DISCONNECTED) {
                throw new ConflictException("Meter is disconnected and cannot be used for billing");
            }
            throw new ConflictException("Meter is inactive and cannot be used for billing");
        }
        return meter;
    }

    private Meter findMeterOrThrow(Long id) {
        return meterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meter not found with id: " + id));
    }

    private Meter findMeterWithCustomerOrThrow(Long id) {
        return meterRepository.findByIdWithCustomer(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meter not found with id: " + id));
    }

    private Customer findCustomerOrThrow(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
    }

    private void validateUniqueMeterNumber(String meterNumber, Long excludeId) {
        boolean exists = excludeId == null
                ? meterRepository.existsByMeterNumber(meterNumber)
                : meterRepository.existsByMeterNumberAndIdNot(meterNumber, excludeId);

        if (exists) {
            throw new ConflictException("Meter with this meter number already exists");
        }
    }

    private String normalizeSearchParam(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
