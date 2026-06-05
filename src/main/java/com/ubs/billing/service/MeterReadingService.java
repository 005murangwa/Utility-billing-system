package com.ubs.billing.service;

import com.ubs.billing.dto.mapper.MeterReadingMapper;
import com.ubs.billing.dto.request.CreateMeterReadingRequest;
import com.ubs.billing.dto.request.UpdateMeterReadingRequest;
import com.ubs.billing.dto.response.MeterReadingResponse;
import com.ubs.billing.dto.response.PageResponse;
import com.ubs.billing.entity.AuditAction;
import com.ubs.billing.entity.Meter;
import com.ubs.billing.entity.MeterReading;
import com.ubs.billing.util.AuditEntityNames;
import com.ubs.billing.exception.BadRequestException;
import com.ubs.billing.exception.ConflictException;
import com.ubs.billing.exception.ResourceNotFoundException;
import com.ubs.billing.repository.MeterReadingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ubs.billing.util.SearchQueryUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class MeterReadingService {

    private final MeterReadingRepository meterReadingRepository;
    private final MeterService meterService;
    private final AuditLogService auditLogService;

    @Transactional
    public MeterReadingResponse createReading(CreateMeterReadingRequest request) {
        Meter meter = meterService.findActiveMeterOrThrow(request.getMeterId());

        validateReadingRules(
                request.getPreviousReading(),
                request.getCurrentReading(),
                request.getReadingDate(),
                request.getMonth(),
                request.getYear(),
                request.getMeterId(),
                null);

        MeterReading reading = MeterReading.builder()
                .meter(meter)
                .previousReading(request.getPreviousReading())
                .currentReading(request.getCurrentReading())
                .readingDate(request.getReadingDate())
                .month(request.getMonth())
                .year(request.getYear())
                .build();

        try {
            MeterReading savedReading = meterReadingRepository.save(reading);
            auditLogService.log(AuditAction.CREATE, AuditEntityNames.METER_READING, savedReading.getId());
            return MeterReadingMapper.toResponse(findReadingWithMeterOrThrow(savedReading.getId()));
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("A reading already exists for this meter in the specified month and year");
        }
    }

    @Transactional
    public MeterReadingResponse updateReading(Long id, UpdateMeterReadingRequest request) {
        MeterReading reading = findReadingWithMeterOrThrow(id);
        Meter meter = meterService.findActiveMeterOrThrow(request.getMeterId());

        validateReadingRules(
                request.getPreviousReading(),
                request.getCurrentReading(),
                request.getReadingDate(),
                request.getMonth(),
                request.getYear(),
                request.getMeterId(),
                id);

        reading.setMeter(meter);
        reading.setPreviousReading(request.getPreviousReading());
        reading.setCurrentReading(request.getCurrentReading());
        reading.setReadingDate(request.getReadingDate());
        reading.setMonth(request.getMonth());
        reading.setYear(request.getYear());

        try {
            MeterReading savedReading = meterReadingRepository.save(reading);
            return MeterReadingMapper.toResponse(findReadingWithMeterOrThrow(savedReading.getId()));
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("A reading already exists for this meter in the specified month and year");
        }
    }

    @Transactional
    public void deleteReading(Long id) {
        MeterReading reading = findReadingOrThrow(id);
        meterReadingRepository.delete(reading);
    }

    @Transactional(readOnly = true)
    public MeterReadingResponse getReadingById(Long id) {
        return MeterReadingMapper.toResponse(findReadingWithMeterOrThrow(id));
    }

    @Transactional(readOnly = true)
    public MeterReading getReadingForBilling(Long id) {
        return findReadingWithMeterOrThrow(id);
    }

    @Transactional(readOnly = true)
    public PageResponse<MeterReadingResponse> listReadings(
            Long meterId,
            Integer month,
            Integer year,
            String meterNumber,
            Pageable pageable) {

        Page<MeterReadingResponse> page = meterReadingRepository
                .searchReadings(meterId, month, year, SearchQueryUtils.toOptionalLikePattern(meterNumber), pageable)
                .map(MeterReadingMapper::toResponse);

        return PageResponse.from(page);
    }

    private void validateReadingRules(
            BigDecimal previousReading,
            BigDecimal currentReading,
            LocalDate readingDate,
            Integer month,
            Integer year,
            Long meterId,
            Long excludeId) {

        if (currentReading.compareTo(previousReading) <= 0) {
            throw new BadRequestException("Current reading must be greater than previous reading");
        }

        if (readingDate.getMonthValue() != month || readingDate.getYear() != year) {
            throw new BadRequestException("Month and year must match the reading date");
        }

        if (readingDate.isAfter(LocalDate.now())) {
            throw new BadRequestException("Reading date cannot be in the future");
        }

        boolean duplicateExists = excludeId == null
                ? meterReadingRepository.existsByMeterIdAndMonthAndYear(meterId, month, year)
                : meterReadingRepository.existsByMeterIdAndMonthAndYearAndIdNot(meterId, month, year, excludeId);

        if (duplicateExists) {
            throw new ConflictException("A reading already exists for this meter in the specified month and year");
        }
    }

    private MeterReading findReadingOrThrow(Long id) {
        return meterReadingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meter reading not found with id: " + id));
    }

    private MeterReading findReadingWithMeterOrThrow(Long id) {
        return meterReadingRepository.findByIdWithMeter(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meter reading not found with id: " + id));
    }
}
