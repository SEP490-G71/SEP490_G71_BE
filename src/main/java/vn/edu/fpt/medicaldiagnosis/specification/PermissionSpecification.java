package vn.edu.fpt.medicaldiagnosis.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.fpt.medicaldiagnosis.entity.Permission;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PermissionSpecification {
    public static Specification<Permission> buildSpecification(Map<String, String> filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            List<String> excludedParams = List.of("page", "size", "sortBy", "sortDir");

            filters.forEach((field, value) -> {
                if (value != null && !value.isEmpty() && !excludedParams.contains(field)) {
                    try {
                        switch (field) {
                            case "name", "description", "groupName" -> 
                                predicates.add(cb.like(cb.lower(root.get(field)), "%" + value.toLowerCase() + "%"));
                        }
                    } catch (IllegalArgumentException | IllegalStateException ex) {
                        // Bỏ qua lỗi
                    }
                }
            });

            // Lọc soft delete
            predicates.add(cb.isNull(root.get("deletedAt")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
