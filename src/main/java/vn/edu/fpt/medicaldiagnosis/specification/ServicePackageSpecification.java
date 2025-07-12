package vn.edu.fpt.medicaldiagnosis.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.fpt.medicaldiagnosis.entity.ServicePackage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ServicePackageSpecification {

    public static Specification<ServicePackage> buildSpecification(Map<String, String> filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            List<String> excludedParams = List.of("page", "size", "sortBy", "sortDir");

            filters.forEach((field, value) -> {
                if (value != null && !value.isBlank() && !excludedParams.contains(field)) {
                    String normalizedValue = value.trim().toLowerCase();

                    switch (field) {
                        case "tenantId":
                        case "status":
                        case "billingType":
                            predicates.add(cb.equal(cb.lower(root.get(field)), normalizedValue));
                            break;

                        case "packageName":
                        case "description":
                            predicates.add(cb.like(cb.lower(root.get(field)), "%" + normalizedValue + "%"));
                            break;

                        default:
                            break; // bỏ qua field không hợp lệ
                    }
                }
            });

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
