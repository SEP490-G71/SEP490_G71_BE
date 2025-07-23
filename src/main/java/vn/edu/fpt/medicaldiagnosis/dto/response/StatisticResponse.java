package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticResponse {
    private long paidPackageCount;   // Số gói thu phí đã bán
    private double totalRevenue;       // Tổng doanh thu (đồng)
    private long tenantCount;        // Số tenant đã đăng ký
}
