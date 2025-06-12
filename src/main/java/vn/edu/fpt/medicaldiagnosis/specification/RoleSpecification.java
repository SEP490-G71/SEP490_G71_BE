package vn.edu.fpt.medicaldiagnosis.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.fpt.medicaldiagnosis.entity.Role;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RoleSpecification {

    public static Specification<Role> buildSpecification(Map<String, String> filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            List<String> excludedParams = List.of("page", "size", "sortBy", "sortDir");

            filters.forEach((field, value) -> {
                if (value != null && !value.isEmpty() && !excludedParams.contains(field)) {
                    switch (field) {
                        case "name":
                            predicates.add(cb.like(cb.lower(root.get("name")), "%" + value.toLowerCase() + "%"));
                            break;
                        default:
                            // Nếu field tồn tại trong entity thì mới tạo predicate
                            if (root.getModel().getAttributes().stream()
                                    .anyMatch(attr -> attr.getName().equals(field))) {
                                predicates.add(cb.like(cb.lower(root.get(field)), "%" + value.toLowerCase() + "%"));
                            }
                    }
                }
            });

            // Lọc soft delete
            predicates.add(cb.isNull(root.get("deletedAt")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
