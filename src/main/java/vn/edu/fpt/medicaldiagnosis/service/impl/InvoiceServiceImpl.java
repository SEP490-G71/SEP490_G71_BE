package vn.edu.fpt.medicaldiagnosis.service.impl;

import jakarta.transaction.Transactional;
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
import vn.edu.fpt.medicaldiagnosis.dto.request.PayInvoiceRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.UpdateInvoiceRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.InvoiceResponse;
import vn.edu.fpt.medicaldiagnosis.entity.*;
import vn.edu.fpt.medicaldiagnosis.enums.InvoiceStatus;
import vn.edu.fpt.medicaldiagnosis.enums.MedicalOrderStatus;
import vn.edu.fpt.medicaldiagnosis.enums.MedicalRecordStatus;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.InvoiceMapper;
import vn.edu.fpt.medicaldiagnosis.repository.*;
import vn.edu.fpt.medicaldiagnosis.service.InvoiceService;
import vn.edu.fpt.medicaldiagnosis.specification.InvoiceSpecification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    InvoiceRepository invoiceRepository;
    MedicalOrderRepository medicalOrderRepository;
    MedicalRecordRepository medicalRecordRepository;
    StaffRepository staffRepository;
    InvoiceMapper invoiceMapper;
    InvoiceItemRepository invoiceItemRepository;
    MedicalServiceRepository medicalServiceRepository;
    @Override
    public InvoiceResponse payInvoice(PayInvoiceRequest request) {
        log.info("Service: pay invoice");
        // 1. Lấy invoice và kiểm tra trạng thái
        Invoice invoice = invoiceRepository.findByIdAndDeletedAtIsNull(request.getInvoiceId())
                .orElseThrow(() -> new AppException(ErrorCode.INVOICE_NOT_FOUND));

        if (InvoiceStatus.PAID.equals(invoice.getStatus())) {
            throw new AppException(ErrorCode.INVOICE_ALREADY_PAID);
        }
        // 2. Lấy thông tin thu ngân xác nhận
        Staff staff = staffRepository.findByIdAndDeletedAtIsNull(request.getStaffId())
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));
        // 3. Cập nhật invoice
        invoice.setPaymentType(request.getPaymentType());
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setConfirmedBy(staff);
        invoice.setConfirmedAt(LocalDateTime.now());
        invoiceRepository.save(invoice);
        // 4. Cập nhật trạng thái các medical order liên quan
        List<MedicalOrder> orders =
                medicalOrderRepository.findAllByInvoiceItemInvoiceId(invoice.getId());
        Set<MedicalRecord> relatedRecords = new HashSet<>();

        for (MedicalOrder order : orders) {
            order.setStatus(MedicalOrderStatus.WAITING);
            relatedRecords.add(order.getMedicalRecord());
        }
        medicalOrderRepository.saveAll(orders);

        // 5. Cập nhật trạng thái của 1 medical record duy nhất
        if (relatedRecords.isEmpty()) {
            throw new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND);
        }

        if (relatedRecords.size() > 1) {
            throw new AppException(ErrorCode.MULTIPLE_MEDICAL_RECORDS_FOUND);
        }

        MedicalRecord record = relatedRecords.iterator().next();
        record.setStatus(MedicalRecordStatus.TESTING);
        medicalRecordRepository.save(record);
        log.info("Invoice {} has been marked as PAID by staff {}", invoice.getId(), staff.getId());

        return invoiceMapper.toInvoiceResponse(invoice);
    }

    @Override
    public Page<InvoiceResponse> getInvoicesPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir) {
        String sortColumn = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortColumn).ascending() : Sort.by(sortColumn).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Invoice> spec = InvoiceSpecification.buildSpecification(filters);
        Page<Invoice> pageResult = invoiceRepository.findAll(spec, pageable);

        return pageResult.map(invoiceMapper::toInvoiceResponse);
    }

    @Override
    public InvoiceResponse updateInvoiceItems(UpdateInvoiceRequest request) {
        log.info("Service: update invoice items (smart diff)");

        Invoice invoice = invoiceRepository.findByIdAndDeletedAtIsNull(request.getInvoiceId())
                .orElseThrow(() -> new AppException(ErrorCode.INVOICE_NOT_FOUND));

        if (InvoiceStatus.PAID.equals(invoice.getStatus())) {
            throw new AppException(ErrorCode.INVOICE_ALREADY_PAID);
        }

        Staff staff = staffRepository.findByIdAndDeletedAtIsNull(request.getStaffId())
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        // Load hiện tại
        List<InvoiceItem> currentItems = invoiceItemRepository.findAllByInvoiceId(invoice.getId());
        Map<String, InvoiceItem> currentItemMap = currentItems.stream()
                .collect(Collectors.toMap(item -> item.getService().getId(), Function.identity()));

        List<MedicalOrder> allOrders = medicalOrderRepository.findAllByInvoiceItemIdIn(
                currentItems.stream().map(InvoiceItem::getId).toList()
        );

//        Map<String, List<MedicalOrder>> orderMapByItemId = allOrders.stream()
//                .collect(Collectors.groupingBy(order -> order.getInvoiceItem().getId()));

        List<InvoiceItem> itemsToDelete = new ArrayList<>();
        List<InvoiceItem> itemsToKeep = new ArrayList<>();
//        List<InvoiceItem> itemsToUpdate = new ArrayList<>();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (UpdateInvoiceRequest.InvoiceItemUpdateRequest newItem : request.getServices()) {
            String serviceId = newItem.getServiceId();
            int newQuantity = newItem.getQuantity();

            MedicalService service = medicalServiceRepository.findByIdAndDeletedAtIsNull(serviceId)
                    .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_SERVICE_NOT_FOUND));

            BigDecimal price = service.getPrice();
            BigDecimal discount = service.getDiscount() != null ? service.getDiscount() : BigDecimal.ZERO;
            BigDecimal vat = service.getVat() != null ? service.getVat() : BigDecimal.ZERO;

            BigDecimal discounted = price.subtract(discount);
            BigDecimal subtotal = discounted.multiply(BigDecimal.valueOf(newQuantity));
            BigDecimal total = subtotal.add(subtotal.multiply(vat).divide(BigDecimal.valueOf(100)));
            totalAmount = totalAmount.add(total);

            if (currentItemMap.containsKey(serviceId)) {
                InvoiceItem oldItem = currentItemMap.get(serviceId);
                if (oldItem.getQuantity() == newQuantity) {
                    // Giữ lại
                    itemsToKeep.add(oldItem);
                    continue;
                } else {
                    // Cần xoá và tạo lại
                    itemsToDelete.add(oldItem);
                }
            }

            // Tạo mới
            InvoiceItem created = invoiceItemRepository.save(InvoiceItem.builder()
                    .invoice(invoice)
                    .service(service)
                    .name(service.getName())
                    .quantity(newQuantity)
                    .price(price)
                    .discount(discount)
                    .vat(vat)
                    .total(total)
                    .build());

            for (int i = 0; i < newQuantity; i++) {
                medicalOrderRepository.save(MedicalOrder.builder()
                        .medicalRecord(allOrders.isEmpty() ? null : allOrders.get(0).getMedicalRecord())
                        .service(service)
                        .invoiceItem(created)
                        .createdBy(staff)
                        .status(MedicalOrderStatus.PENDING)
                        .build());
            }
        }

        // Xoá item và order không còn nữa
        for (InvoiceItem item : currentItems) {
            boolean inRequest = request.getServices().stream()
                    .anyMatch(i -> i.getServiceId().equals(item.getService().getId()));
            if (!inRequest) {
                itemsToDelete.add(item);
            }
        }

        // Xoá medical orders trước
        List<String> deleteItemIds = itemsToDelete.stream().map(InvoiceItem::getId).toList();
        List<MedicalOrder> ordersToDelete = allOrders.stream()
                .filter(order -> deleteItemIds.contains(order.getInvoiceItem().getId()))
                .toList();
        medicalOrderRepository.deleteAll(ordersToDelete);
        invoiceItemRepository.deleteAll(itemsToDelete);

        // Cập nhật lại total
        invoice.setAmount(totalAmount);
        invoiceRepository.save(invoice);

        return invoiceMapper.toInvoiceResponse(invoice);
    }

}
