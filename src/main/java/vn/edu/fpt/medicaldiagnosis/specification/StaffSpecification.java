package vn.edu.fpt.medicaldiagnosis.specification;


import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.fpt.medicaldiagnosis.common.DataUtil;
import vn.edu.fpt.medicaldiagnosis.entity.Staff;
import vn.edu.fpt.medicaldiagnosis.enums.Level;
import vn.edu.fpt.medicaldiagnosis.enums.Specialty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StaffSpecification {
    public static Specification<Staff> buildSpecification(Map<String, String> filters) {
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
                                predicates.add(cb.like(cb.lower(root.get("fullName")), "%" + normalizedValue + "%"));
                                break;
                            case "level":
                                try {
                                    Level levelEnum = Level.valueOf(value.toUpperCase());
                                    predicates.add(cb.equal(root.get("level"), levelEnum));
                                } catch (IllegalArgumentException e) {
                                    // Nếu enum không hợp lệ, có thể bỏ qua hoặc log
                                }
                                break;
                            case "specialty":
                                try {
                                    Specialty specialtyEnum = Specialty.valueOf(value.toUpperCase());
                                    predicates.add(cb.equal(root.get("specialty"), specialtyEnum));
                                } catch (IllegalArgumentException e) {
                                    // Nếu enum không hợp lệ, có thể bỏ qua hoặc log
                                }
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
