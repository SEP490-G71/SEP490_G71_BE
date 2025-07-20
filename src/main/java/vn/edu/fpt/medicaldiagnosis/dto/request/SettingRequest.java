package vn.edu.fpt.medicaldiagnosis.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettingRequest {
    @NotBlank(message = "HOSPITAL_NAME_REQUIRED")
    @Size(min = 3, max = 100, message = "HOSPITAL_NAME_LENGTH")
    private String hospitalName;

    @NotBlank(message = "HOSPITAL_PHONE_REQUIRED")
    @Size(min = 10, max = 10, message = "HOSPITAL_PHONE_LENGTH")
    private String hospitalPhone;

    @NotBlank(message = "HOSPITAL_EMAIL_REQUIRED")
    @Email(message = "HOSPITAL_EMAIL_INVALID")
    private String hospitalEmail;

    private String hospitalAddress;

    private String bankAccountNumber;

    private String bankCode;

    @NotNull(message = "LATEST_CHECK_IN_MINUTES_REQUIRED")
    @Max(value = 60, message = "LATEST_CHECK_IN_MINUTES_MAX_60")
    private Integer latestCheckInMinutes;


    @NotEmpty(message = "PAGING_SIZE_REQUIRED")
    private List<Integer> paginationSizeList;

    @NotNull(message = "QUEUE_OPEN_TIME_REQUIRED")
    private LocalTime queueOpenTime;

    @NotNull(message = "QUEUE_CLOSE_TIME_REQUIRED")
    private LocalTime queueCloseTime;

    @NotNull(message = "MIN_BOOKING_DAYS_BEFORE_REQUIRED")
    @Min(value = 0, message = "MIN_BOOKING_DAYS_BEFORE_MIN_0")
    @Max(value = 30, message = "MIN_BOOKING_DAYS_BEFORE_MAX_30")
    private Integer minBookingDaysBefore;

    @NotNull(message = "MIN_LEAVE_DAYS_BEFORE_REQUIRED")
    @Min(value = 0, message = "MIN_LEAVE_DAYS_BEFORE_MIN_0")
    @Max(value = 30, message = "MIN_LEAVE_DAYS_BEFORE_MAX_30")
    private Integer minLeaveDaysBefore;

    @AssertTrue(message = "QUEUE_OPEN_TIME_MUST_BE_BEFORE_CLOSE_TIME")
    public boolean isQueueTimeValid() {
        if (queueOpenTime == null || queueCloseTime == null) {
            return true; // đã có NotNull riêng
        }
        return queueOpenTime.isBefore(queueCloseTime);
    }
}

