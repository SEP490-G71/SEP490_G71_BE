package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SettingResponse {
    private String hospitalName;
    private String hospitalPhone;
    private String hospitalAddress;
    private String bankAccountNumber;
    private String bankCode;
    private List<Integer> paginationSizeList;
}

