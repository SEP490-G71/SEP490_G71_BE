package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class SettingResponse {
    private String hospitalName;
    private String hospitalPhone;
    private String hospitalEmail;
    private String hospitalAddress;
    private String bankAccountNumber;
    private String bankCode;
    private Integer latestCheckInMinutes;
    private List<Integer> paginationSizeList;
    private LocalTime queueOpenTime;
    private LocalTime queueCloseTime;
    private Integer minBookingDaysBefore;
    private Integer minLeaveDaysBefore;
    private Integer docShiftQuota;
    private BigDecimal monthlyTargetRevenue;
}

