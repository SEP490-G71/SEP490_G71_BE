package vn.edu.fpt.medicaldiagnosis.service;

import org.springframework.data.domain.Page;
import vn.edu.fpt.medicaldiagnosis.dto.request.ShiftRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ShiftResponse;

import java.util.List;
import java.util.Map;

public interface ShiftService {
    ShiftResponse create(ShiftRequest request);
    ShiftResponse update(String id, ShiftRequest request);
    void delete(String id);
    ShiftResponse getById(String id);
    List<ShiftResponse> getAll();
    Page<ShiftResponse> getShiftsPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir);
}
