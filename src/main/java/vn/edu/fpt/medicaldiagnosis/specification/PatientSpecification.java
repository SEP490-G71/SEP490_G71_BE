package vn.edu.fpt.medicaldiagnosis.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.fpt.medicaldiagnosis.common.DataUtil;
import vn.edu.fpt.medicaldiagnosis.entity.Patient;
import vn.edu.fpt.medicaldiagnosis.enums.Gender;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PatientSpecification {

    public static Specification<Patient> buildSpecification(Map<String, String> filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            List<String> excludedParams = List.of("page", "size", "sortBy", "sortDir");

            filters.forEach((field, value) -> {
                if (value != null && !value.isEmpty() && !excludedParams.contains(field)) {
                    String normalizedValue = DataUtil.normalizeForSearch(value);

                    try {
                        switch (field) {
                            case "name":
                                predicates.add(cb.like(cb.lower(root.get("fullName")), "%" + normalizedValue + "%"));
                                break;

                            case "gender":
                                predicates.add(cb.equal(root.get("gender"), Gender.valueOf(value.toUpperCase())));
                                break;
                            case "phone":
                                predicates.add(cb.like(cb.lower(root.get("phone")), "%" + normalizedValue + "%"));
                                break;
                            case "patientCode":
                                predicates.add(cb.like(cb.lower(root.get("patientCode")), "%" + normalizedValue + "%"));
                                break;
                            default:
                                if (root.getModel().getAttributes().stream().anyMatch(a -> a.getName().equals(field))) {
                                    predicates.add(cb.like(cb.lower(root.get(field)), "%" + value.toLowerCase() + "%"));
                                }
                        }
                    } catch (Exception ignored) {}
                }
            });

            predicates.add(cb.isNull(root.get("deletedAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
