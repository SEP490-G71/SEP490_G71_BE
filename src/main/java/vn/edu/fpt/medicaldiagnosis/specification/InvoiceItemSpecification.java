package vn.edu.fpt.medicaldiagnosis.specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.fpt.medicaldiagnosis.entity.Invoice;
import vn.edu.fpt.medicaldiagnosis.entity.InvoiceItem;
import jakarta.persistence.criteria.Predicate;
import vn.edu.fpt.medicaldiagnosis.enums.InvoiceStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InvoiceItemSpecification {
    public static Specification<InvoiceItem> buildSpecification(Map<String, String> filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<InvoiceItem, Invoice> invoiceJoin = root.join("invoice", JoinType.INNER);

            // Chỉ tính hoá đơn đã thanh toán
            predicates.add(cb.equal(invoiceJoin.get("status"), InvoiceStatus.PAID));

            filters.forEach((field, value) -> {
                if (value != null && !value.isEmpty()) {
                    switch (field) {
                        case "serviceCode" -> predicates.add(cb.like(cb.lower(root.get("serviceCode")), "%" + value.toLowerCase() + "%"));
                        case "name" -> predicates.add(cb.like(cb.lower(root.get("name")), "%" + value.toLowerCase() + "%"));
                        case "fromDate" -> {
                            LocalDate fromDate = LocalDate.parse(value);
                            predicates.add(cb.greaterThanOrEqualTo(invoiceJoin.get("createdAt"), fromDate.atStartOfDay()));
                        }
                        case "toDate" -> {
                            LocalDate toDate = LocalDate.parse(value);
                            predicates.add(cb.lessThan(invoiceJoin.get("createdAt"), toDate.plusDays(1).atStartOfDay()));
                        }
                    }
                }
            });

            predicates.add(cb.isNull(root.get("deletedAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
