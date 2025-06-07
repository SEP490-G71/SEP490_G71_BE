package vn.edu.fpt.medicaldiagnosis.specification;

import org.springframework.data.jpa.domain.Specification;
import vn.edu.fpt.medicaldiagnosis.entity.MedicalService;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
public class MedicalServiceSpecification {

    public static Specification<MedicalService> buildSpecification(Map<String, String> filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            List<String> excludedParams = List.of("page", "size", "sortBy", "sortDir");

            filters.forEach((field, value) -> {
                if (value != null && !value.isEmpty() && !excludedParams.contains(field)) {
                    try {
                        switch (field) {
                            case "name":
                                predicates.add(cb.like(cb.lower(root.get("name")), "%" + value.toLowerCase() + "%"));
                                break;
                            case "departmentId":
                                predicates.add(cb.equal(root.get("department").get("id"), value));
                                break;
                            default:
                                // Nếu muốn hỗ trợ thêm field linh hoạt
                                if (root.getModel().getAttributes().stream()
                                        .anyMatch(a -> a.getName().equals(field))) {
                                    predicates.add(cb.like(cb.lower(root.get(field)), "%" + value.toLowerCase() + "%"));
                                }
                                break;
                        }
                    } catch (IllegalArgumentException ex) {
                        // Bỏ qua lỗi: UUID không hợp lệ, field không tồn tại
                        // Có thể log nếu cần
                    }
                }
            });

            // Lọc soft delete
            predicates.add(cb.isNull(root.get("deletedAt")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

