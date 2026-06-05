package com.ubs.billing.service;

import com.ubs.billing.dto.mapper.TariffMapper;
import com.ubs.billing.dto.request.CreateTariffRequest;
import com.ubs.billing.dto.request.UpdateTariffRequest;
import com.ubs.billing.dto.response.PageResponse;
import com.ubs.billing.dto.response.TariffResponse;
import com.ubs.billing.entity.AuditAction;
import com.ubs.billing.entity.MeterType;
import com.ubs.billing.entity.Tariff;
import com.ubs.billing.util.AuditEntityNames;
import com.ubs.billing.exception.BadRequestException;
import com.ubs.billing.exception.ResourceNotFoundException;
import com.ubs.billing.repository.TariffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TariffService {

    private final TariffRepository tariffRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public TariffResponse createTariff(CreateTariffRequest request) {
        int nextVersion = tariffRepository.findMaxVersionByMeterType(request.getMeterType()) + 1;

        if (Boolean.TRUE.equals(request.getActive())) {
            tariffRepository.deactivateAllActiveByMeterType(request.getMeterType());
        }

        Tariff tariff = Tariff.builder()
                .meterType(request.getMeterType())
                .rate(request.getRate())
                .serviceCharge(request.getServiceCharge())
                .vat(request.getVat())
                .penaltyRate(request.getPenaltyRate())
                .version(nextVersion)
                .effectiveDate(request.getEffectiveDate())
                .active(request.getActive())
                .build();

        Tariff savedTariff = tariffRepository.save(tariff);
        auditLogService.log(AuditAction.CREATE, AuditEntityNames.TARIFF, savedTariff.getId());
        return TariffMapper.toResponse(savedTariff);
    }

    @Transactional
    public TariffResponse updateTariff(Long id, UpdateTariffRequest request) {
        Tariff existingTariff = findTariffOrThrow(id);
        MeterType meterType = existingTariff.getMeterType();
        int nextVersion = tariffRepository.findMaxVersionByMeterType(meterType) + 1;

        if (Boolean.TRUE.equals(request.getActive())) {
            tariffRepository.deactivateAllActiveByMeterType(meterType);
        }

        Tariff newVersion = Tariff.builder()
                .meterType(meterType)
                .rate(request.getRate())
                .serviceCharge(request.getServiceCharge())
                .vat(request.getVat())
                .penaltyRate(request.getPenaltyRate())
                .version(nextVersion)
                .effectiveDate(request.getEffectiveDate())
                .active(request.getActive())
                .build();

        Tariff savedTariff = tariffRepository.save(newVersion);
        auditLogService.log(
                AuditAction.UPDATE,
                AuditEntityNames.TARIFF,
                savedTariff.getId(),
                "{\"previousTariffId\":\"" + id + "\"}",
                "{\"version\":\"" + savedTariff.getVersion() + "\"}");
        return TariffMapper.toResponse(savedTariff);
    }

    @Transactional
    public TariffResponse deactivateTariff(Long id) {
        Tariff tariff = findTariffOrThrow(id);

        if (!Boolean.TRUE.equals(tariff.getActive())) {
            throw new BadRequestException("Tariff is already inactive");
        }

        tariff.setActive(false);
        return TariffMapper.toResponse(tariffRepository.save(tariff));
    }

    @Transactional(readOnly = true)
    public TariffResponse getTariffById(Long id) {
        return TariffMapper.toResponse(findTariffOrThrow(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<TariffResponse> getTariffs(
            MeterType meterType,
            Boolean active,
            Pageable pageable) {

        Page<TariffResponse> page = tariffRepository
                .searchTariffs(meterType, active, pageable)
                .map(TariffMapper::toResponse);

        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public TariffResponse getLatestActiveTariff(MeterType meterType) {
        return tariffRepository
                .findFirstByMeterTypeAndActiveTrueAndEffectiveDateLessThanEqualOrderByVersionDescEffectiveDateDesc(
                        meterType, LocalDate.now())
                .map(TariffMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active tariff found for meter type: " + meterType));
    }

    @Transactional(readOnly = true)
    public Tariff findActiveTariffForBilling(MeterType meterType, LocalDate billingDate) {
        return tariffRepository
                .findFirstByMeterTypeAndActiveTrueAndEffectiveDateLessThanEqualOrderByVersionDescEffectiveDateDesc(
                        meterType, billingDate)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active tariff found for meter type " + meterType + " on date " + billingDate));
    }

    private Tariff findTariffOrThrow(Long id) {
        return tariffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tariff not found with id: " + id));
    }
}
