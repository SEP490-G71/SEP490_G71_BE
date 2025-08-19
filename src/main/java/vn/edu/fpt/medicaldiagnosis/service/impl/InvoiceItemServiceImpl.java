package vn.edu.fpt.medicaldiagnosis.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.dto.response.InvoiceItemReportItem;
import vn.edu.fpt.medicaldiagnosis.dto.response.InvoiceItemStatisticResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.PagedResponse;
import vn.edu.fpt.medicaldiagnosis.entity.InvoiceItem;
import vn.edu.fpt.medicaldiagnosis.entity.MedicalService;
import vn.edu.fpt.medicaldiagnosis.repository.InvoiceItemRepository;
import vn.edu.fpt.medicaldiagnosis.repository.MedicalServiceRepository;
import vn.edu.fpt.medicaldiagnosis.service.InvoiceItemService;
import vn.edu.fpt.medicaldiagnosis.specification.InvoiceItemSpecification;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InvoiceItemServiceImpl implements InvoiceItemService {
    InvoiceItemRepository invoiceItemRepository;
    MedicalServiceRepository medicalServiceRepository;
    @Override
    public InvoiceItemStatisticResponse getInvoiceItemStatistics(Map<String, String> filters,
                                                                 int page, int size,
                                                                 String sortBy, String sortDir) {
        log.info("Service: Start getInvoiceItemStatistics, filters={}, page={}, size={}, sortBy={}, sortDir={}",
                filters, page, size, sortBy, sortDir);

        // Chuẩn hoá page/size
        page = Math.max(page, 0);
        size = size > 0 ? size : 10;

        // Sort fields hợp lệ
        List<String> validSortFields = List.of("serviceCode", "name", "price", "total", "totalUsage", "totalRevenue");
        if (!validSortFields.contains(sortBy)) {
            log.warn("Invalid sortBy value. Fallback to 'serviceCode'");
            sortBy = "serviceCode";
        }

        // Lấy filter hợp lệ
        String nameFilter = opt(filters.get("name"));
        String codeFilter = opt(filters.get("serviceCode"));
        String departmentId = opt(filters.get("departmentId"));
        boolean hasContentFilter = hasText(nameFilter) || hasText(codeFilter);

        // Áp dụng spec
        Specification<InvoiceItem> spec = InvoiceItemSpecification.buildSpecification(Map.of(
                "name", nameFilter,
                "serviceCode", codeFilter,
                "departmentId", departmentId,
                "fromDate", opt(filters.get("fromDate")),
                "toDate", opt(filters.get("toDate"))
        ));
        List<InvoiceItem> items = invoiceItemRepository.findAll(spec);
        log.info("Fetched {} invoice items after applying filters", items.size());

        // Group theo service
        Map<String, InvoiceItemReportItem> grouped = new HashMap<>();
        for (InvoiceItem item : items) {
            String serviceId = item.getService().getId();

            InvoiceItemReportItem report = grouped.computeIfAbsent(serviceId, k -> InvoiceItemReportItem.builder()
                    .serviceCode(item.getServiceCode())
                    .name(item.getName()) // hoặc item.getService().getName() nếu bạn muốn hiển thị tên từ service
                    .price(nvl(item.getPrice()))
                    .totalUsage(0L)
                    .totalRevenue(BigDecimal.ZERO)
                    .totalOriginal(BigDecimal.ZERO)
                    .totalDiscount(BigDecimal.ZERO)
                    .totalVat(BigDecimal.ZERO)
                    .build());

            long quantity = item.getQuantity();
            BigDecimal price = nvl(item.getPrice());
            BigDecimal discountPercent = nvl(item.getDiscount());
            BigDecimal vatPercent = nvl(item.getVat());
            BigDecimal quantityBig = BigDecimal.valueOf(quantity);

            BigDecimal originalTotal = price.multiply(quantityBig);
            BigDecimal discountAmount = originalTotal.multiply(discountPercent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal vatAmount = originalTotal.multiply(vatPercent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            report.setTotalUsage(report.getTotalUsage() + quantity);
            report.setTotalRevenue(report.getTotalRevenue().add(nvl(item.getTotal()))); // giữ theo nghiệp vụ hiện tại
            report.setTotalOriginal(report.getTotalOriginal().add(originalTotal));
            report.setTotalDiscount(report.getTotalDiscount().add(discountAmount));
            report.setTotalVat(report.getTotalVat().add(vatAmount));
        }
        log.info("Grouped into {} unique service items", grouped.size());

        // BÙ dịch vụ chỉ khi KHÔNG có filter nội dung (để dashboard tổng quát hiển thị đủ list)
        if (!hasContentFilter) {
            List<MedicalService> allServices = hasText(departmentId)
                    ? medicalServiceRepository.findAllByDepartment_Id(departmentId)
                    : medicalServiceRepository.findAll();

            for (MedicalService service : allServices) {
                grouped.putIfAbsent(service.getId(),
                        InvoiceItemReportItem.builder()
                                .serviceCode(service.getServiceCode())
                                .name(service.getName())
                                .price(nvl(service.getPrice()))
                                .totalUsage(0L)
                                .totalRevenue(BigDecimal.ZERO)
                                .totalOriginal(BigDecimal.ZERO)
                                .totalDiscount(BigDecimal.ZERO)
                                .totalVat(BigDecimal.ZERO)
                                .build());
            }
        }

        // (Phòng xa) lọc hậu kiểm theo name/serviceCode nếu có filter nội dung
        if (hasContentFilter) {
            final String nameLower = nameFilter.toLowerCase();
            final String codeLower = codeFilter.toLowerCase();
            grouped.values().removeIf(it -> {
                String n = opt(it.getName()).toLowerCase();
                String c = opt(it.getServiceCode()).toLowerCase();
                boolean matchName = hasText(nameLower) && n.contains(nameLower);
                boolean matchCode = hasText(codeLower) && c.contains(codeLower);
                return !(matchName || matchCode);
            });
        }

        // Sort
        List<InvoiceItemReportItem> sorted = new ArrayList<>(grouped.values());
        Comparator<InvoiceItemReportItem> comparator = switch (sortBy) {
            case "serviceCode" -> Comparator.comparing(InvoiceItemReportItem::getServiceCode,
                    Comparator.nullsLast(String::compareToIgnoreCase));
            case "name" -> Comparator.comparing(InvoiceItemReportItem::getName,
                    Comparator.nullsLast(String::compareToIgnoreCase));
            case "price" -> Comparator.comparing(InvoiceItemReportItem::getPrice,
                    Comparator.nullsLast(BigDecimal::compareTo));
            case "total", "totalRevenue" -> Comparator.comparing(InvoiceItemReportItem::getTotalRevenue,
                    Comparator.nullsLast(BigDecimal::compareTo));
            case "totalUsage" -> Comparator.comparingLong(InvoiceItemReportItem::getTotalUsage);
            default -> Comparator.comparing(InvoiceItemReportItem::getServiceCode);
        };
        if ("desc".equalsIgnoreCase(sortDir)) comparator = comparator.reversed();
        sorted.sort(comparator);

        // mostUsedService theo totalUsage (độc lập sort)
        InvoiceItemReportItem mostUsed = grouped.values().stream()
                .filter(i -> i.getTotalUsage() > 0)
                .max(Comparator.comparingLong(InvoiceItemReportItem::getTotalUsage))
                .orElse(null);

        // Tổng hợp + phân trang
        int totalElements = sorted.size();
        int totalPages = (int) Math.ceil(totalElements / (double) size);

        List<InvoiceItemReportItem> pageItems = sorted.stream()
                .skip((long) page * size)       // page 0-based
                .limit(size)
                .toList();

        BigDecimal totalRevenue = sorted.stream()
                .map(InvoiceItemReportItem::getTotalRevenue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalUsage = sorted.stream().mapToLong(InvoiceItemReportItem::getTotalUsage).sum();

        return InvoiceItemStatisticResponse.builder()
                .totalServiceTypes(grouped.size())
                .totalUsage(totalUsage)
                .totalRevenue(totalRevenue)
                .mostUsedService(mostUsed)
                .details(new PagedResponse<>(
                        pageItems,
                        page,
                        size,
                        totalElements,
                        totalPages,
                        (page + 1) * size >= totalElements
                ))
                .build();
    }

    // helpers
    private static boolean hasText(String s) { return s != null && !s.trim().isEmpty(); }
    private static String opt(String s) { return s == null ? "" : s.trim(); }
    private static BigDecimal nvl(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }


}
