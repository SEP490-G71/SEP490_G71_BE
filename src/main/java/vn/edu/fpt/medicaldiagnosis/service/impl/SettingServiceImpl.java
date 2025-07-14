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
import vn.edu.fpt.medicaldiagnosis.repository.SettingRepository;
import vn.edu.fpt.medicaldiagnosis.service.SettingService;

import static lombok.AccessLevel.PRIVATE;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class SettingServiceImpl implements SettingService {
    private final SettingRepository settingRepository;

    @Override
    public SettingResponse getSetting() {
        Setting setting = (Setting) settingRepository.findFirstByOrderByCreatedAtAsc()
                .orElseThrow(() -> new AppException(ErrorCode.SETTING_NOT_FOUND));
        return mapToResponse(setting);
    }

    @Override
    public SettingResponse upsertSetting(SettingRequest request) {
        Setting setting = (Setting) settingRepository.findFirstByOrderByCreatedAtAsc()
                .orElse(Setting.builder().build()); // nếu chưa có thì tạo mới

        setting.setHospitalName(request.getHospitalName());
        setting.setHospitalPhone(request.getHospitalPhone());
        setting.setHospitalAddress(request.getHospitalAddress());
        setting.setBankAccountNumber(request.getBankAccountNumber());
        setting.setBankCode(request.getBankCode());
        setting.setPaginationSizeList(request.getPaginationSizeList());

        settingRepository.save(setting);
        return mapToResponse(setting);
    }

    private SettingResponse mapToResponse(Setting setting) {
        return SettingResponse.builder()
                .hospitalName(setting.getHospitalName())
                .hospitalPhone(setting.getHospitalPhone())
                .hospitalAddress(setting.getHospitalAddress())
                .bankAccountNumber(setting.getBankAccountNumber())
                .bankCode(setting.getBankCode())
                .paginationSizeList(setting.getPaginationSizeList())
                .build();
    }
}
