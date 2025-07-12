package vn.edu.fpt.medicaldiagnosis.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettingRequest {
    private String hospitalName;
    private String hospitalPhone;
    private String hospitalAddress;
    private String bankAccountNumber;
    private String bankCode;
    private List<Integer> paginationSizeList;
}

