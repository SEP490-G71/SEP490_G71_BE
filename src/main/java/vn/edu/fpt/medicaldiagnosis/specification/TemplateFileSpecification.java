package vn.edu.fpt.medicaldiagnosis.specification;

import org.springframework.data.jpa.domain.Specification;
import vn.edu.fpt.medicaldiagnosis.entity.TemplateFile;
import jakarta.persistence.criteria.Predicate;
import vn.edu.fpt.medicaldiagnosis.enums.TemplateFileType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class TemplateFileSpecification {
    public static Specification<TemplateFile> buildSpecification(Map<String, String> filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            List<String> excludedParams = List.of("page", "size", "sortBy", "sortDir");

            filters.forEach((field, value) -> {
                if (value != null && !value.isBlank() && !excludedParams.contains(field)) {
                    String normalizedValue = value.trim().toLowerCase();
                    switch (field) {
                        case "name":
                            predicates.add(cb.like(cb.lower(root.get("name")), "%" + normalizedValue + "%"));
                            break;
                        case "type":
                            try {
                                TemplateFileType typeEnum = TemplateFileType.valueOf(value.toUpperCase());
                                predicates.add(cb.equal(root.get("type"), typeEnum));
                            } catch (IllegalArgumentException ignored) {
                                // Skip invalid enum
                            }
                            break;
                        case "isDefault":
                            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                                predicates.add(cb.equal(root.get("isDefault"), Boolean.parseBoolean(value)));
                            }
                            break;
                        default:
                            if (root.getModel().getAttributes().stream().anyMatch(attr -> attr.getName().equals(field))) {
                                predicates.add(cb.like(cb.lower(root.get(field)), "%" + normalizedValue + "%"));
                            }
                    }
                }
            });

            predicates.add(cb.isNull(root.get("deletedAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
