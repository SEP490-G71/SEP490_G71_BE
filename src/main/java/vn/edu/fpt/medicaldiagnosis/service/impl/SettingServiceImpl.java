package vn.edu.fpt.medicaldiagnosis.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.dto.request.SettingRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.SettingResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Setting;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.SettingMapper;
import vn.edu.fpt.medicaldiagnosis.repository.SettingRepository;
import vn.edu.fpt.medicaldiagnosis.service.SettingService;

import static lombok.AccessLevel.PRIVATE;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class SettingServiceImpl implements SettingService {
    SettingRepository settingRepository;
    SettingMapper settingMapper;
    @Override
    public SettingResponse getSetting() {
        Setting setting = settingRepository.findFirstByOrderByCreatedAtAsc()
                .orElseThrow(() -> new AppException(ErrorCode.SETTING_NOT_FOUND));
        return settingMapper.toResponse(setting);
    }

    @Override
    public SettingResponse upsertSetting(SettingRequest request) {
        log.info("Service: upsert setting {}", request.getMinLeaveDaysBefore());
        Setting setting = settingRepository.findFirstByOrderByCreatedAtAsc()
                .orElse(Setting.builder().build());

        settingMapper.updateSettingFromRequest(request, setting);
        settingRepository.save(setting);
        return settingMapper.toResponse(setting);
    }
}
