package vn.edu.fpt.medicaldiagnosis.service;

import vn.edu.fpt.medicaldiagnosis.dto.request.ServicePackageRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ServicePackageResponse;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface ServicePackageService {

    ServicePackageResponse create(ServicePackageRequest request);

    ServicePackageResponse update(String id, ServicePackageRequest request);

    void delete(String id);

    ServicePackageResponse getById(String id);

    List<ServicePackageResponse> getAll();

    Page<ServicePackageResponse> getServicePackagesPaged(
            Map<String, String> filters,
            int page,
            int size,
            String sortBy,
            String sortDir
    );
}
