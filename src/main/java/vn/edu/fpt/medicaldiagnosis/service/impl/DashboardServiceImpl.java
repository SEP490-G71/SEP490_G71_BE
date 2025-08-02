package vn.edu.fpt.medicaldiagnosis.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.dto.response.DashboardOverviewResponse;
import vn.edu.fpt.medicaldiagnosis.service.DashboardService;

import static lombok.AccessLevel.PRIVATE;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class DashboardServiceImpl implements DashboardService {

    @Override
    public DashboardOverviewResponse getDashboardOverview() {
        log.info("Service: get dashboard overview");
        return null;
    }
}
