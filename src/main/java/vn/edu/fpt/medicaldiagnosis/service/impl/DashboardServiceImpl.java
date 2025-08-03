package vn.edu.fpt.medicaldiagnosis.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.dto.response.*;
import vn.edu.fpt.medicaldiagnosis.enums.InvoiceStatus;
import vn.edu.fpt.medicaldiagnosis.repository.*;
import vn.edu.fpt.medicaldiagnosis.service.DashboardService;
import vn.edu.fpt.medicaldiagnosis.service.SettingService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class DashboardServiceImpl implements DashboardService {
    InvoiceRepository invoiceRepository;
    MedicalRecordRepository medicalRecordRepository;
    PatientRepository patientRepository;
    InvoiceItemRepository invoiceItemRepository;
    WorkScheduleRepository workScheduleRepository;
    SettingService settingService;
    @Override
    public DashboardOverviewResponse getDashboardOverview() {
        log.info("Service: get dashboard overview");

        // Tổng số hóa đơn
        long totalInvoices = invoiceRepository.countByDeletedAtIsNull();

        // Hóa đơn đã thanh toán
        int paidInvoices = invoiceRepository.countByStatus(InvoiceStatus.PAID);

        // Tổng doanh thu
        long totalRevenue = invoiceRepository.sumTotalAmountByStatus(InvoiceStatus.PAID);

        // Tổng bệnh án
        long totalMedicalRecords = medicalRecordRepository.countByDeletedAtIsNull();

        // Bệnh nhân mới trong tháng
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfTomorrow = LocalDate.now().plusDays(1).atStartOfDay();

        int newPatientsThisMonth = patientRepository.countByCreatedAtBetween(startOfMonth, startOfTomorrow);

        // Top dịch vụ có doanh thu cao nhất
        List<TopServiceResponse> topServices = invoiceItemRepository.findTopServicesByRevenue(PageRequest.of(0, 5));

        // Doanh thu theo tháng
        List<MonthlyCountResponse> revenueStatsRaw = invoiceRepository.getMonthlyRevenueStats();

        ChartDataResponse monthlyRevenueStats = ChartDataResponse.builder()
                .labels(revenueStatsRaw.stream().map(MonthlyCountResponse::getMonth).toList())
                .data(revenueStatsRaw.stream().map(MonthlyCountResponse::getTotal).toList())
                .build();

        // Hóa đơn theo tháng
        List<MonthlyCountResponse> rawStats = invoiceRepository.getMonthlyInvoiceStats();

        ChartDataResponse monthlyInvoiceStats = ChartDataResponse.builder()
                .labels(rawStats.stream().map(MonthlyCountResponse::getMonth).toList())
                .data(rawStats.stream().map(MonthlyCountResponse::getTotal).toList())
                .build();

        // Hiệu suất làm việc (ví dụ: top 5 nhân viên có nhiều ca nhất/tháng)
        List<WorkScheduleReportResponseInterface> projectionList = workScheduleRepository.getWorkScheduleReportThisMonth();

        // Convert projection → class
        List<WorkScheduleReportResponse> workPerformance = projectionList.stream()
                .map(p -> WorkScheduleReportResponse.builder()
                        .staffId(p.getStaffId())
                        .staffName(p.getStaffName())
                        .staffCode(p.getStaffCode())
                        .totalShifts(p.getTotalShifts())
                        .attendedShifts(p.getAttendedShifts())
                        .leaveShifts(p.getLeaveShifts())
                        .attendanceRate(p.getAttendanceRate())
                        .leaveRate(p.getLeaveRate())
                        .build())
                .toList();

        // Danh sách sinh nhật hôm nay
        List<BirthdayResponse> birthdaysToday = patientRepository.findPatientsWithBirthdayToday()
                .stream()
                .map(p -> BirthdayResponse.builder()
                        .fullName(p.getFullName())
                        .patientCode(p.getPatientCode())
                        .email(p.getEmail())
                        .phone(p.getPhone())
                        .dob(p.getDob())
                        .build())
                .toList();
        // Target tháng hiện tại (giả sử hardcoded hoặc lấy từ config hệ thống)
        SettingResponse setting = settingService.getSetting();
        BigDecimal targetAmount = setting.getMonthlyTargetRevenue(); // BigDecimal
        BigDecimal currentAmount = BigDecimal.valueOf(totalRevenue); // long → BigDecimal

        BigDecimal progress = currentAmount
                .divide(targetAmount, 2, RoundingMode.HALF_UP)  // chia và làm tròn
                .multiply(BigDecimal.valueOf(100));              // nhân với 100



        return DashboardOverviewResponse.builder()
                .totalInvoices(totalInvoices)
                .paidInvoices(paidInvoices)
                .totalRevenue(totalRevenue)
                .totalMedicalRecords(totalMedicalRecords)
                .newPatientsThisMonth(newPatientsThisMonth)
                .topServices(topServices)
                .monthlyRevenueStats(monthlyRevenueStats)
                .monthlyInvoiceStats(monthlyInvoiceStats)
                .workPerformance(workPerformance)
                .birthdaysToday(birthdaysToday)
                .monthlyTarget(MonthlyTargetResponse.builder()
                        .targetAmount(targetAmount)
                        .currentAmount(currentAmount)
                        .progressPercent(progress)
                        .build())
                .build();
    }
}
