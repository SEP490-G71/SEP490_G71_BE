package vn.edu.fpt.medicaldiagnosis.specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.fpt.medicaldiagnosis.entity.Department;
import vn.edu.fpt.medicaldiagnosis.entity.Invoice;
import vn.edu.fpt.medicaldiagnosis.entity.InvoiceItem;
import jakarta.persistence.criteria.Predicate;
import vn.edu.fpt.medicaldiagnosis.entity.MedicalService;
import vn.edu.fpt.medicaldiagnosis.enums.InvoiceStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InvoiceItemSpecification {

    public static Specification<InvoiceItem> buildSpecification(Map<String, String> filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Join cần thiết
            Join<InvoiceItem, Invoice> invoiceJoin = root.join("invoice", JoinType.INNER);
            Join<InvoiceItem, MedicalService> serviceJoin = root.join("service", JoinType.LEFT);
            Join<MedicalService, Department> departmentJoin = serviceJoin.join("department", JoinType.LEFT);

            // Chỉ tính hoá đơn đã thanh toán
            predicates.add(cb.equal(invoiceJoin.get("status"), InvoiceStatus.PAID));

            // deletedAt
            predicates.add(cb.isNull(root.get("deletedAt")));

            String serviceCode = opt(filters.get("serviceCode"));
            String name = opt(filters.get("name"));
            String fromDate = opt(filters.get("fromDate"));
            String toDate = opt(filters.get("toDate"));
            String departmentId = opt(filters.get("departmentId"));

            if (hasText(serviceCode)) {
                predicates.add(cb.like(cb.lower(root.get("serviceCode")), "%" + serviceCode.toLowerCase() + "%"));
            }

            if (hasText(name)) {
                // name khớp InvoiceItem.name hoặc MedicalService.name
                Predicate byItemName = cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
                Predicate byServiceName = cb.like(cb.lower(serviceJoin.get("name")), "%" + name.toLowerCase() + "%");
                predicates.add(cb.or(byItemName, byServiceName));
            }

            if (hasText(fromDate)) {
                LocalDate d = LocalDate.parse(fromDate);
                predicates.add(cb.greaterThanOrEqualTo(invoiceJoin.get("createdAt"), d.atStartOfDay()));
            }

            if (hasText(toDate)) {
                LocalDate d = LocalDate.parse(toDate);
                predicates.add(cb.lessThan(invoiceJoin.get("createdAt"), d.plusDays(1).atStartOfDay()));
            }

            if (hasText(departmentId)) {
                predicates.add(cb.equal(departmentJoin.get("id"), departmentId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean hasText(String s) { return s != null && !s.trim().isEmpty(); }
    private static String opt(String s) { return s == null ? "" : s.trim(); }
}

