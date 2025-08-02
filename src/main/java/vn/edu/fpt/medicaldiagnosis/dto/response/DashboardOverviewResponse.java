package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardOverviewResponse {
    private long totalInvoices;
    private int paidInvoices;
    private long totalRevenue;
    private long totalMedicalRecords;
    private int newPatientsThisMonth;

    private List<TopServiceResponse> topServices;
    private ChartDataResponse monthlyRevenueStats;
    private ChartDataResponse monthlyInvoiceStats;

    private List<WorkScheduleReportResponse> workPerformance;
    private List<BirthdayResponse> birthdaysToday;

    private MonthlyTargetResponse monthlyTarget;
}
