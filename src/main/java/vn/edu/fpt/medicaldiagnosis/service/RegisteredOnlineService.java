package vn.edu.fpt.medicaldiagnosis.service;

import org.springframework.data.domain.Page;
import vn.edu.fpt.medicaldiagnosis.dto.request.RegisteredOnlineRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.RegisteredOnlineResponse;

import java.util.Map;

public interface RegisteredOnlineService {

    RegisteredOnlineResponse create(RegisteredOnlineRequest request);

    Page<RegisteredOnlineResponse> getPaged(
            Map<String, String> filters,
            int page,
            int size,
            String sortBy,
            String sortDir
    );

    RegisteredOnlineResponse getById(String id);

    void delete(String id);

    RegisteredOnlineResponse update(String id, RegisteredOnlineRequest request);

    RegisteredOnlineResponse updateStatus(String id, RegisteredOnlineRequest request);
}
