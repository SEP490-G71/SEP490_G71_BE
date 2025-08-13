package vn.edu.fpt.medicaldiagnosis.service;

import org.springframework.data.domain.Page;
import vn.edu.fpt.medicaldiagnosis.dto.request.AlertCreateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.AlertBasicResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.AlertResponse;

import java.util.Map;

public interface MetricAlertService {

    AlertResponse createAlert(AlertCreateRequest req);

    Page<AlertBasicResponse> getAlertsPaged(Map<String, String> filters, int page, int size,
                                            String sortBy, String sortDir);

    AlertResponse getAlertById(String id);
}
