package vn.edu.fpt.medicaldiagnosis.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.fpt.medicaldiagnosis.entity.Department;
import vn.edu.fpt.medicaldiagnosis.enums.DepartmentType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DepartmentSpecification {
    public static Specification<Department> buildSpecification(Map<String, String> filters) {
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
                            case "type":
                                try {
                                    DepartmentType typeEnum = DepartmentType.valueOf(value.toUpperCase());
                                    predicates.add(cb.equal(root.get("type"), typeEnum));
                                } catch (IllegalArgumentException e) {
                                    // Bỏ qua hoặc log lỗi enum không hợp lệ
                                }
                                break;
                            default:
                                if (root.getModel().getAttributes().stream()
                                        .anyMatch(a -> a.getName().equals(field))) {
                                    predicates.add(cb.like(cb.lower(root.get(field)), "%" + value.toLowerCase() + "%"));
                                }
                                break;
                        }
                    } catch (IllegalArgumentException | IllegalStateException ex) {
                        // Bỏ qua lỗi không tìm thấy trường hoặc enum không hợp lệ
                    }
                }
            });

            // Lọc soft delete
            predicates.add(cb.isNull(root.get("deletedAt")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
