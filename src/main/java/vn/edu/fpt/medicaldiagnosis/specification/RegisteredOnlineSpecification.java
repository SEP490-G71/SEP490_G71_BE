package vn.edu.fpt.medicaldiagnosis.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.fpt.medicaldiagnosis.common.DataUtil;
import vn.edu.fpt.medicaldiagnosis.entity.RegisteredOnline;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RegisteredOnlineSpecification {

    public static Specification<RegisteredOnline> buildSpecification(Map<String, String> filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            List<String> excludedParams = List.of("page", "size", "sortBy", "sortDir");

            filters.forEach((field, value) -> {
                if (value != null && !value.isEmpty() && !excludedParams.contains(field)) {
                    String normalizedValue = DataUtil.normalizeForSearch(value);

                    try {
                        switch (field) {
                            case "fullName":
                            case "email":
                            case "phoneNumber":
                            case "message":
                                predicates.add(cb.like(cb.lower(root.get(field)), "%" + normalizedValue + "%"));
                                break;
                            case "registeredAt":
                                predicates.add(cb.equal(root.get(field), value)); // Có thể cần chuyển sang LocalDate
                                break;
                            default:
                                // Tránh lỗi nếu field không hợp lệ
                                break;
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            });

            predicates.add(cb.isNull(root.get("deletedAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
