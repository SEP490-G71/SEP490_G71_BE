package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.Data;

@Data
public class DashboardOverviewResponse {
    private int totalInvoices;
    private int paidInvoices;
    private long totalRevenue;
    private int totalMedicalRecords;
    private int newPatientsThisMonth;

//    private List<TopServiceDto> topServices;
//    private ChartData monthlyRevenueStats;
//    private ChartData monthlyInvoiceStats;
//
//    private List<WorkPerformanceDto> workPerformance;
//    private List<BirthdayDto> birthdaysToday;
//
//    private MonthlyTargetDto monthlyTarget;
}
