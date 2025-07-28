package vn.edu.fpt.medicaldiagnosis.specification;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.fpt.medicaldiagnosis.common.DataUtil;
import vn.edu.fpt.medicaldiagnosis.entity.TransactionHistory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TransactionHistorySpecification {

    public static Specification<TransactionHistory> buildSpecification(Map<String, String> filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            List<String> excludedParams = List.of("page", "size", "sortBy", "sortDir");

            filters.forEach((field, value) -> {
                if (value != null && !value.isEmpty() && !excludedParams.contains(field)) {
                    String normalizedValue = DataUtil.normalizeForSearch(value);

                    try {
                        switch (field) {
                            case "packageName":
                            case "tenantCode":
                                predicates.add(cb.like(cb.lower(root.get(field)), "%" + normalizedValue + "%"));
                                break;
                            case "startDate":
                                LocalDate startDate = LocalDate.parse(value);
                                Expression<LocalDate> dbStartDate = cb.function("DATE", LocalDate.class, root.get("startDate"));
                                predicates.add(cb.greaterThanOrEqualTo(dbStartDate, startDate));
                                break;
                            case "endDate":
                                LocalDate endDate = LocalDate.parse(value);
                                Expression<LocalDate> dbEndDate = cb.function("DATE", LocalDate.class, root.get("endDate"));
                                predicates.add(cb.lessThanOrEqualTo(dbEndDate, endDate));
                                break;
                            default:
                                if (root.getModel().getAttributes().stream().anyMatch(a -> a.getName().equals(field))) {
                                    predicates.add(cb.like(cb.lower(root.get(field)), "%" + normalizedValue + "%"));
                                }
                                break;
                        }
                    } catch (IllegalArgumentException ignored) {
                        // Bỏ qua nếu field không hợp lệ
                    }
                }
            });

            predicates.add(cb.isNull(root.get("deletedAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
