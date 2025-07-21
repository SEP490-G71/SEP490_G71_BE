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
    public InvoiceItemStatisticResponse getInvoiceItemStatistics(Map<String, String> filters, int page, int size, String sortBy, String sortDir) {
        log.info("Service: Start getInvoiceItemStatistics, filters={}, page={}, size={}, sortBy={}, sortDir={}", filters, page, size, sortBy, sortDir);

        List<String> validSortFields = List.of("serviceCode", "name", "price", "quantity", "total", "totalUsage", "totalRevenue");
        if (!validSortFields.contains(sortBy)) {
            sortBy = "serviceCode";
            log.warn("Invalid sortBy value. Fallback to 'serviceCode'");
        }

        Specification<InvoiceItem> spec = InvoiceItemSpecification.buildSpecification(filters);
        List<InvoiceItem> items = invoiceItemRepository.findAll(spec);
        log.info("Fetched {} invoice items after applying filters", items.size());

        Map<String, InvoiceItemReportItem> grouped = new HashMap<>();
        for (InvoiceItem item : items) {
            String key = item.getServiceCode() + ":" + item.getName() + ":" + item.getPrice();
            InvoiceItemReportItem report = grouped.computeIfAbsent(key, k -> InvoiceItemReportItem.builder()
                    .serviceCode(item.getServiceCode())
                    .name(item.getName())
                    .price(item.getPrice())
                    .totalUsage(0L)
                    .totalRevenue(BigDecimal.ZERO)
                    .totalOriginal(BigDecimal.ZERO)
                    .totalDiscount(BigDecimal.ZERO)
                    .totalVat(BigDecimal.ZERO)
                    .build());

            long quantity = item.getQuantity();
            BigDecimal price = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
            BigDecimal discountPercent = item.getDiscount() != null ? item.getDiscount() : BigDecimal.ZERO;
            BigDecimal vatPercent = item.getVat() != null ? item.getVat() : BigDecimal.ZERO;

            BigDecimal quantityBig = BigDecimal.valueOf(quantity);
            BigDecimal originalTotal = price.multiply(quantityBig);
            BigDecimal discountAmount = originalTotal.multiply(discountPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal vatAmount = originalTotal.multiply(vatPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            report.setTotalUsage(report.getTotalUsage() + quantity);
            report.setTotalRevenue(report.getTotalRevenue().add(item.getTotal())); // vẫn giữ như cũ
            report.setTotalOriginal(report.getTotalOriginal().add(originalTotal));
            report.setTotalDiscount(report.getTotalDiscount().add(discountAmount));
            report.setTotalVat(report.getTotalVat().add(vatAmount));
        }


        log.info("Grouped into {} unique service items", grouped.size());

        List<MedicalService> allServices = medicalServiceRepository.findAll();
        for (MedicalService service : allServices) {
            String key = service.getServiceCode() + ":" + service.getName() + ":" + service.getPrice();
            if (!grouped.containsKey(key)) {
                grouped.put(key, InvoiceItemReportItem.builder()
                        .serviceCode(service.getServiceCode())
                        .name(service.getName())
                        .price(service.getPrice())
                        .totalUsage(0L)
                        .totalRevenue(BigDecimal.ZERO)
                        .totalOriginal(BigDecimal.ZERO)
                        .totalDiscount(BigDecimal.ZERO)
                        .totalVat(BigDecimal.ZERO)
                        .build());
            }
        }

        List<InvoiceItemReportItem> sorted = new ArrayList<>(grouped.values());
        Comparator<InvoiceItemReportItem> comparator = switch (sortBy) {
            case "serviceCode" -> Comparator.comparing(InvoiceItemReportItem::getServiceCode, Comparator.nullsLast(String::compareToIgnoreCase));
            case "name" -> Comparator.comparing(InvoiceItemReportItem::getName, Comparator.nullsLast(String::compareToIgnoreCase));
            case "price" -> Comparator.comparing(InvoiceItemReportItem::getPrice, Comparator.nullsLast(BigDecimal::compareTo));
            case "total" -> Comparator.comparing(InvoiceItemReportItem::getTotalRevenue, Comparator.nullsLast(BigDecimal::compareTo));
            case "totalRevenue", "totalUsage" -> Comparator.comparingLong(InvoiceItemReportItem::getTotalUsage);
            default -> Comparator.comparing(InvoiceItemReportItem::getServiceCode);
        };

        if ("desc".equalsIgnoreCase(sortDir)) {
            comparator = comparator.reversed();
        }

        sorted.sort(comparator);

        int totalElements = sorted.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        List<InvoiceItemReportItem> pageItems = sorted.stream()
                .skip((long) page * size)
                .limit(size)
                .toList();

        return InvoiceItemStatisticResponse.builder()
                .totalServiceTypes(grouped.size())
                .totalUsage(sorted.stream().mapToLong(InvoiceItemReportItem::getTotalUsage).sum())
                .totalRevenue(sorted.stream().map(InvoiceItemReportItem::getTotalRevenue).reduce(BigDecimal.ZERO, BigDecimal::add))
                .mostUsedService(sorted.stream().filter(i -> i.getTotalUsage() > 0).findFirst().orElse(null))
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

}
