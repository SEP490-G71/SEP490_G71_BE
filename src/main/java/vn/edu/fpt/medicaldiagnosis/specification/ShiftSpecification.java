package vn.edu.fpt.medicaldiagnosis.specification;


import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.fpt.medicaldiagnosis.common.DataUtil;
import vn.edu.fpt.medicaldiagnosis.entity.Shift;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShiftSpecification {
    public static Specification<Shift> buildSpecification(Map<String, String> filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Danh sách các param không phải thuộc tính entity
            List<String> excludedParams = List.of("page", "size", "sortBy", "sortDir");

            filters.forEach((field, value) -> {
                if (value != null && !value.isEmpty() && !excludedParams.contains(field)) {
                    try {
                        String normalizedValue = DataUtil.normalizeForSearch(value);

                        switch (field) {
                            case "name":
                                predicates.add(cb.like(cb.lower(root.get("name")), "%" + normalizedValue + "%"));
                                break;
                            default:
                                // Kiểm tra xem field có tồn tại trong entity không để tránh lỗi
                                if (root.getModel().getAttributes().stream()
                                        .anyMatch(a -> a.getName().equals(field))) {
                                    predicates.add(cb.like(cb.lower(root.get(field)), "%" + normalizedValue + "%"));
                                }
                                break;
                        }
                    } catch (IllegalArgumentException | IllegalStateException ex) {
                        // Bỏ qua các lỗi không tìm thấy trường hoặc enum không hợp lệ
                    }
                }
            });

            // Filter soft delete
            predicates.add(cb.isNull(root.get("deletedAt")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
