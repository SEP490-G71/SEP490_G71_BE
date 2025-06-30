package vn.edu.fpt.medicaldiagnosis.specification;

import org.springframework.data.jpa.domain.Specification;
import vn.edu.fpt.medicaldiagnosis.entity.Account;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AccountSpecification {
    public static Specification<Account> buildSpecification(Map<String, String> filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Danh sách các param không phải là field thực sự (nếu bạn dùng chung Map cho cả page, sort)
            List<String> excludedParams = List.of("page", "size", "sortBy", "sortDir");

            filters.forEach((field, value) -> {
                if (value != null && !value.isBlank() && !excludedParams.contains(field)) {
                    try {
                        String normalizedValue = value.trim().toLowerCase(); // Hoặc DataUtil.normalizeForSearch(value)

                        switch (field) {
                            case "username":
                                predicates.add(cb.like(cb.lower(root.get("username")), "%" + normalizedValue + "%"));
                                break;

                            case "isActive":
                                boolean isActive = Boolean.parseBoolean(value);
                                predicates.add(cb.equal(root.get("isActive"), isActive));
                                break;

                            default:
                                // Kiểm tra nếu field tồn tại trong entity → áp dụng lọc mờ LIKE
                                if (root.getModel().getAttributes().stream()
                                        .anyMatch(attr -> attr.getName().equals(field))) {
                                    predicates.add(cb.like(cb.lower(root.get(field)), "%" + normalizedValue + "%"));
                                }
                                break;
                        }

                    } catch (IllegalArgumentException | IllegalStateException ex) {
                        // Có thể log hoặc bỏ qua lỗi
                    }
                }
            });

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
