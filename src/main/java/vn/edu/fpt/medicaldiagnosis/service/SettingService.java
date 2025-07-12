package vn.edu.fpt.medicaldiagnosis.service;

import vn.edu.fpt.medicaldiagnosis.dto.request.SettingRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.SettingResponse;

public interface SettingService {
    SettingResponse getSetting();
    SettingResponse upsertSetting(SettingRequest request);
}
