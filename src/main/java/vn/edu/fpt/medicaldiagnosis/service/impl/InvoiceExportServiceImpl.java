package vn.edu.fpt.medicaldiagnosis.service.impl;


import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.common.DataUtil;
import vn.edu.fpt.medicaldiagnosis.dto.response.InvoiceResponse;
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
public class InvoiceExportServiceImpl {
    public ByteArrayInputStream exportToExcel(List<InvoiceResponse> invoices) throws IOException {
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
                BigDecimal amount = invoice.getAmount();
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

}
