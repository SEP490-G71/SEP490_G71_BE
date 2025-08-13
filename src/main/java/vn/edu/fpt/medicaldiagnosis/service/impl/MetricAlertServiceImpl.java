package vn.edu.fpt.medicaldiagnosis.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.dto.request.AlertCreateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.AlertBasicResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.AlertResponse;
import vn.edu.fpt.medicaldiagnosis.entity.MetricAlert;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.repository.MetricAlertRepository;
import vn.edu.fpt.medicaldiagnosis.service.MetricAlertService;
import vn.edu.fpt.medicaldiagnosis.specification.MetricAlertSpecification;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MetricAlertServiceImpl implements MetricAlertService {
    MetricAlertRepository  metricAlertRepository;

    @Override
    public AlertResponse createAlert(AlertCreateRequest req) {
        MetricAlert alert = MetricAlert.builder()
                .metricCode(req.getMetricCode())
                .periodStart(req.getPeriodStart())
                .periodEnd(req.getPeriodEnd())
                .level(req.getLevel())
                .actualValue(req.getActualValue())
                .targetValue(req.getTargetValue())
                .diffPct(req.getDiffPct())
                .momPct(req.getMomPct())
                .reason(req.getReason())
                .payloadJson(req.getPayloadJson())
                .build();

        MetricAlert saved = metricAlertRepository.save(alert);

        return toDto(saved);
    }

    @Override
    public Page<AlertBasicResponse> getAlertsPaged(Map<String, String> filters, int page, int size,
                                                   String sortBy, String sortDir) {
        String sortColumn = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Sort sort = "asc".equalsIgnoreCase(sortDir) ? Sort.by(sortColumn).ascending() : Sort.by(sortColumn).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<MetricAlert> spec = MetricAlertSpecification.buildSpecification(filters);
        Page<MetricAlert> result = metricAlertRepository.findAll(spec, pageable);

        return result.map(this::toBasicDto);
    }

    @Override
    public AlertResponse getAlertById(String id) {
        MetricAlert entity = metricAlertRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.METRIC_ALERT_NOT_FOUND, "MetricAlert không tồn tại: " + id));
        return toDto(entity);
    }

    private AlertBasicResponse toBasicDto(MetricAlert e) {
        if (e == null) return null;
        return AlertBasicResponse.builder()
                .id(e.getId())
                .metricCode(e.getMetricCode())
                .periodStart(e.getPeriodStart())
                .periodEnd(e.getPeriodEnd())
                .level(e.getLevel())
                .actualValue(e.getActualValue())
                .targetValue(e.getTargetValue())
                .diffPct(e.getDiffPct())
                .momPct(e.getMomPct())
                .build();
    }

    private AlertResponse toDto(MetricAlert entity) {
        if (entity == null) return null;
        return AlertResponse.builder()
                .id(entity.getId())
                .metricCode(entity.getMetricCode())
                .periodStart(entity.getPeriodStart())
                .periodEnd(entity.getPeriodEnd())
                .level(entity.getLevel())
                .actualValue(entity.getActualValue())
                .targetValue(entity.getTargetValue())
                .diffPct(entity.getDiffPct())
                .momPct(entity.getMomPct())
                .reason(entity.getReason())
                .payloadJson(entity.getPayloadJson())
                .build();
    }
}
