package vn.edu.fpt.medicaldiagnosis.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.fpt.medicaldiagnosis.entity.MedicalRecord;
import vn.edu.fpt.medicaldiagnosis.enums.MedicalRecordStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MedicalRecordByRoomSpecification {
    public static Specification<MedicalRecord> buildSpecification(Map<String, String> filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            List<String> excludedParams = List.of("page", "size", "sortBy", "sortDir");

            filters.forEach((field, value) -> {
                if (value != null && !value.isBlank() && !excludedParams.contains(field)) {
                    try {
                        switch (field) {
                            case "medicalRecordCode":
                                predicates.add(cb.like(
                                        cb.lower(root.get("medicalRecordCode")),
                                        "%" + value.toLowerCase() + "%"
                                ));
                                break;

                            case "patientId":
                                predicates.add(cb.equal(root.get("patient").get("id"), value));
                                break;

                            case "patientCode":
                                predicates.add(cb.like(
                                        cb.lower(root.get("patient").get("patientCode")),
                                        "%" + value.toLowerCase() + "%"
                                ));
                                break;

                            case "patientName":
                                predicates.add(cb.like(
                                        cb.lower(root.get("patient").get("fullName")),
                                        "%" + value.toLowerCase() + "%"
                                ));
                                break;
                            case "patientPhone":
                                predicates.add(cb.like(
                                        cb.lower(root.get("patient").get("phone")),
                                        "%" + value.toLowerCase() + "%"
                                ));
                                break;
                            case "status":
                                try {
                                    MedicalRecordStatus status = MedicalRecordStatus.valueOf(value.toUpperCase());
                                    predicates.add(cb.equal(root.get("status"), status));
                                } catch (IllegalArgumentException ignored) {
                                    // Ignore invalid enum
                                }
                                break;

                            case "fromDate":
                                LocalDate fromDate = LocalDate.parse(value);
                                predicates.add(cb.greaterThanOrEqualTo(
                                        root.get("createdAt"), fromDate.atStartOfDay()));
                                break;

                            case "toDate":
                                LocalDate toDate = LocalDate.parse(value);
                                predicates.add(cb.lessThan(
                                        root.get("createdAt"), toDate.plusDays(1).atStartOfDay()));
                                break;

                            case "roomNumber":
                                predicates.add(cb.equal(root.get("visit").get("roomNumber"), value));
                                break;

                            default:
                                break;
                        }
                    } catch (Exception e) {
                        // Ignore parsing errors and invalid fields
                    }
                }
            });

            // Ensure soft-deleted records are excluded
            predicates.add(cb.isNull(root.get("deletedAt")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
