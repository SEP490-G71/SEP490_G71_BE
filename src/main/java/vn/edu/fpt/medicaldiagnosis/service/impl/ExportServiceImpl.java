package vn.edu.fpt.medicaldiagnosis.service.impl;


import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.common.DataUtil;
import vn.edu.fpt.medicaldiagnosis.dto.response.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class ExportServiceImpl {
    public ByteArrayInputStream exportInvoiceToExcel(List<InvoiceResponse> invoices) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Invoices");

            // Header
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Mã hóa đơn", "Bệnh nhân", "Tổng tiền", "Phương thức thanh toán", "Trạng thái", "Nhân viên thu", "Ngày thanh tóan", "Ngày tạo"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                CellStyle style = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                style.setFont(font);
                cell.setCellStyle(style);
            }

            // Format
            int rowIdx = 1;
            NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

            BigDecimal totalAmount = BigDecimal.ZERO;
            int validInvoices = 0;

            for (InvoiceResponse invoice : invoices) {
                Row row = sheet.createRow(rowIdx++);

                // Mã hóa đơn
                row.createCell(0).setCellValue(DataUtil.safeString(invoice.getInvoiceCode()));

                // Bệnh nhân
                row.createCell(1).setCellValue(DataUtil.safeString(invoice.getPatientName()));

                // Tổng tiền
                BigDecimal amount = invoice.getTotal();
                row.createCell(2).setCellValue(amount != null ? currencyFormatter.format(amount) : "");
                totalAmount = totalAmount.add(amount != null ? amount : BigDecimal.ZERO);

                // Phương thức thanh toán
                String paymentType = invoice.getPaymentType();
                row.createCell(3).setCellValue(
                        paymentType != null ? DataUtil.getPaymentTypeVietnamese(paymentType) : ""
                );

                // Trạng thái
                String status = invoice.getStatus() != null ? invoice.getStatus().toString() : "";
                row.createCell(4).setCellValue(DataUtil.getStatusVietnamese(status));
                if ("PAID".equalsIgnoreCase(status)) {
                    validInvoices++;
                }
                // Nhân viên thu
                row.createCell(5).setCellValue( invoice.getConfirmedBy() != null ? DataUtil.safeString(invoice.getConfirmedBy()) : "");
                // Ngày thu
                row.createCell(6).setCellValue(
                        invoice.getConfirmedAt() != null ? invoice.getConfirmedAt().format(dateFormatter) : ""
                );

                // Ngày tạo
                row.createCell(7).setCellValue(
                        invoice.getCreatedAt() != null ? invoice.getCreatedAt().format(dateFormatter) : ""
                );
            }

            // Dòng thống kê
            rowIdx++;
            Row totalRow = sheet.createRow(rowIdx++);
            totalRow.createCell(0).setCellValue("Tổng số hóa đơn:");
            totalRow.createCell(1).setCellValue(invoices.size());

            Row validRow = sheet.createRow(rowIdx++);
            validRow.createCell(0).setCellValue("Số hóa đơn hợp lệ:");
            validRow.createCell(1).setCellValue(validInvoices);

            Row amountRow = sheet.createRow(rowIdx++);
            amountRow.createCell(0).setCellValue("Tổng tiền hóa đơn:");
            amountRow.createCell(1).setCellValue(currencyFormatter.format(totalAmount));

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    public ByteArrayInputStream exportInvoiceItemToExcel(List<InvoiceItemReportItem> items, InvoiceItemStatisticResponse stats) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Invoice Items");

            // Header
            String[] headers = {"Mã dịch vụ", "Tên dịch vụ", "Giá", "Tổng lượt sử dụng", "Tổng doanh thu"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                CellStyle style = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                style.setFont(font);
                cell.setCellStyle(style);
            }

            // Format
            NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            int rowIdx = 1;

            for (InvoiceItemReportItem item : items) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(item.getServiceCode());
                row.createCell(1).setCellValue(item.getName());
                row.createCell(2).setCellValue(currencyFormatter.format(item.getPrice()));
                row.createCell(3).setCellValue(item.getTotalUsage());
                row.createCell(4).setCellValue(currencyFormatter.format(item.getTotalRevenue()));
            }

            // Summary
            rowIdx++;
            Row totalTypesRow = sheet.createRow(rowIdx++);
            totalTypesRow.createCell(0).setCellValue("Tổng loại dịch vụ:");
            totalTypesRow.createCell(1).setCellValue(stats.getTotalServiceTypes());

            Row totalUsageRow = sheet.createRow(rowIdx++);
            totalUsageRow.createCell(0).setCellValue("Tổng lượt sử dụng:");
            totalUsageRow.createCell(1).setCellValue(stats.getTotalUsage());

            Row totalRevenueRow = sheet.createRow(rowIdx++);
            totalRevenueRow.createCell(0).setCellValue("Tổng doanh thu:");
            totalRevenueRow.createCell(1).setCellValue(currencyFormatter.format(stats.getTotalRevenue()));

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    public ByteArrayInputStream exportPatientBirthdayToExcel(List<PatientResponse> patients) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Bệnh nhân sinh nhật tháng này");

            // Header
            String[] headers = {"Mã bệnh nhân", "Họ tên", "Giới tính", "Ngày sinh", "Số điện thoại", "Email"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                CellStyle style = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                style.setFont(font);
                cell.setCellStyle(style);
            }

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            // Dữ liệu bệnh nhân
            int rowIdx = 1;
            for (PatientResponse patient : patients) {
                System.out.println("Patient: " + patient.getPatientCode());
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(DataUtil.safeString(patient.getPatientCode()));
                row.createCell(1).setCellValue(DataUtil.safeString(patient.getFullName()));
                row.createCell(2).setCellValue(DataUtil.getGenderVietnamese(String.valueOf(patient.getGender())));
                row.createCell(3).setCellValue(patient.getDob() != null ? patient.getDob().format(dateFormatter) : "");
                row.createCell(4).setCellValue(DataUtil.safeString(patient.getPhone()));
                row.createCell(5).setCellValue(DataUtil.safeString(patient.getEmail()));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    public ByteArrayInputStream exportWorkScheduleToExcel(
            List<WorkScheduleReportResponse> items,
            WorkScheduleStatisticResponse stats) throws IOException {

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Báo cáo ca làm việc");

            // ===== Styles =====
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            CellStyle numberStyle = workbook.createCellStyle(); // số nguyên
            DataFormat df = workbook.createDataFormat();
            numberStyle.setDataFormat(df.getFormat("0"));

            // ===== Header =====
            // [NEW]: thêm 2 cột lateShifts, lateRate
            String[] headers = {
                    "Mã nhân viên", "Tên nhân viên",
                    "Tổng số ca", "Số ca đi làm", "Số ca nghỉ",
                    "Số ca đi muộn",               // [NEW]
                    "Tỷ lệ đi làm (%)", "Tỷ lệ nghỉ (%)", "Tỷ lệ đi muộn (%)" // [NEW]
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Freeze header
            sheet.createFreezePane(0, 1);

            // ===== Body =====
            int rowIdx = 1;
            for (WorkScheduleReportResponse item : items) {
                Row row = sheet.createRow(rowIdx++);

                // text
                row.createCell(0).setCellValue(item.getStaffCode());
                row.createCell(1).setCellValue(item.getStaffName());

                // numbers
                Cell cTotal = row.createCell(2);
                cTotal.setCellValue(item.getTotalShifts());
                cTotal.setCellStyle(numberStyle);

                Cell cAtt = row.createCell(3);
                cAtt.setCellValue(item.getAttendedShifts());
                cAtt.setCellStyle(numberStyle);

                Cell cLeave = row.createCell(4);
                cLeave.setCellValue(item.getLeaveShifts());
                cLeave.setCellStyle(numberStyle);

                // [NEW] lateShifts
                Cell cLate = row.createCell(5);
                cLate.setCellValue(item.getLateShifts());
                cLate.setCellStyle(numberStyle);

                // rates: ghi chuỗi kèm "%"
                row.createCell(6).setCellValue(formatPercent(item.getAttendanceRate()));
                row.createCell(7).setCellValue(formatPercent(item.getLeaveRate()));
                row.createCell(8).setCellValue(formatPercent(item.getLateRate())); // [NEW]
            }

            // ===== Summary =====
            rowIdx++; // cách 1 dòng
            Row totalStaffRow = sheet.createRow(rowIdx++);
            totalStaffRow.createCell(0).setCellValue("Tổng số nhân viên:");
            totalStaffRow.createCell(1).setCellValue(safeInt(stats.getTotalStaffs()));

            Row totalShiftRow = sheet.createRow(rowIdx++);
            totalShiftRow.createCell(0).setCellValue("Tổng số ca làm:");
            totalShiftRow.createCell(1).setCellValue(safeInt(stats.getTotalShifts()));

            Row attendedRow = sheet.createRow(rowIdx++);
            attendedRow.createCell(0).setCellValue("Tổng số ca đi làm:");
            attendedRow.createCell(1).setCellValue(safeInt(stats.getAttendedShifts()));

            Row leaveRow = sheet.createRow(rowIdx++);
            leaveRow.createCell(0).setCellValue("Tổng số ca nghỉ:");
            leaveRow.createCell(1).setCellValue(safeInt(stats.getLeaveShifts()));

            // [NEW] tổng số ca đi muộn
            Row lateRow = sheet.createRow(rowIdx++);
            lateRow.createCell(0).setCellValue("Tổng số ca đi muộn:");
            lateRow.createCell(1).setCellValue(safeInt(stats.getLateShifts()));

            Row rateRow = sheet.createRow(rowIdx++);
            rateRow.createCell(0).setCellValue("Tỷ lệ đi làm trung bình:");
            rateRow.createCell(1).setCellValue(formatPercent(stats.getAttendanceRate()));

            // ===== Autosize =====
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    /**
     * Chuẩn hóa hiển thị phần trăm 2 chữ số thập phân, kèm dấu %.
     * Input hệ thống đang trả về theo "điểm phần trăm" (ví dụ 11.11 nghĩa là 11.11%), nên không chia 100 ở đây.
     */
    private String formatPercent(Double value) {
        double v = (value == null) ? 0.0 : value;
        return String.format("%.2f%%", v);
    }

    private long safeInt(Long value) {
        return value == null ? 0 : value;
    }


    public ByteArrayInputStream exportMedicalServiceFeedbackStatisticsToExcel(List<ServiceFeedBackResponse> feedbackList,
                                                                              long totalFeedbacks,
                                                                              BigDecimal averageSatisfaction) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Service Feedbacks");

            // Header
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Mã dịch vụ", "Tên dịch vụ", "Mô tả", "Khoa", "Tổng phản hồi", "Trung bình mức độ hài lòng"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                CellStyle style = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                style.setFont(font);
                cell.setCellStyle(style);
            }

            // Nội dung
            int rowIdx = 1;
            for (ServiceFeedBackResponse feedback : feedbackList) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(feedback.getServiceCode());
                row.createCell(1).setCellValue(feedback.getName());
                row.createCell(2).setCellValue(feedback.getDescription());
                row.createCell(3).setCellValue(feedback.getDepartment() != null ? feedback.getDepartment().getName() : "");
                row.createCell(4).setCellValue(feedback.getTotalFeedbacks());
                row.createCell(5).setCellValue(feedback.getAverageSatisfaction().doubleValue());
            }

            // Dòng thống kê
            rowIdx++;
            Row totalServiceRow = sheet.createRow(rowIdx++);
            totalServiceRow.createCell(0).setCellValue("Tổng số dịch vụ:");
            totalServiceRow.createCell(1).setCellValue(feedbackList.size());

            Row totalFeedbackRow = sheet.createRow(rowIdx++);
            totalFeedbackRow.createCell(0).setCellValue("Tổng số phản hồi:");
            totalFeedbackRow.createCell(1).setCellValue(totalFeedbacks);

            Row avgSatisfactionRow = sheet.createRow(rowIdx++);
            avgSatisfactionRow.createCell(0).setCellValue("Trung bình mức độ hài lòng:");
            avgSatisfactionRow.createCell(1).setCellValue(averageSatisfaction.doubleValue());

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }


    public ByteArrayInputStream exportStaffFeedbackStatisticsToExcel(
            List<StaffFeedbackItemResponse> feedbackList,
            long totalFeedbacks,
            BigDecimal averageSatisfaction) throws IOException {

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Staff Feedbacks");

            // Header
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Mã NV", "Họ tên", "Email", "SĐT", "Giới tính", "Khoa", "Tổng phản hồi", "Trung bình mức độ hài lòng"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                CellStyle style = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                style.setFont(font);
                cell.setCellStyle(style);
            }

            // Body
            int rowIdx = 1;
            for (StaffFeedbackItemResponse feedback : feedbackList) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(feedback.getStaffCode());
                row.createCell(1).setCellValue(feedback.getFullName());
                row.createCell(2).setCellValue(feedback.getEmail());
                row.createCell(3).setCellValue(feedback.getPhone());
                row.createCell(4).setCellValue(feedback.getGender() != null ? feedback.getGender().name() : "");
                row.createCell(5).setCellValue(feedback.getDepartment() != null ? feedback.getDepartment().getName() : "");
                row.createCell(6).setCellValue(feedback.getTotalFeedbacks());
                row.createCell(7).setCellValue(feedback.getAverageSatisfaction().doubleValue());
            }

            // Footer - Statistics
            rowIdx++;
            Row totalStaffRow = sheet.createRow(rowIdx++);
            totalStaffRow.createCell(0).setCellValue("Tổng số nhân viên:");
            totalStaffRow.createCell(1).setCellValue(feedbackList.size());

            Row totalFeedbackRow = sheet.createRow(rowIdx++);
            totalFeedbackRow.createCell(0).setCellValue("Tổng số phản hồi:");
            totalFeedbackRow.createCell(1).setCellValue(totalFeedbacks);

            Row avgSatisfactionRow = sheet.createRow(rowIdx++);
            avgSatisfactionRow.createCell(0).setCellValue("Trung bình mức độ hài lòng:");
            avgSatisfactionRow.createCell(1).setCellValue(averageSatisfaction.doubleValue());

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

}
