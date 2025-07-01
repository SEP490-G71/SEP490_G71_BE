package vn.edu.fpt.medicaldiagnosis.specification;

import org.springframework.data.jpa.domain.Specification;
import vn.edu.fpt.medicaldiagnosis.entity.MedicalRecord;
import jakarta.persistence.criteria.Predicate;
import vn.edu.fpt.medicaldiagnosis.enums.MedicalRecordStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MedicalRecordSpecification {
    public static Specification<MedicalRecord> buildSpecification(Map<String, String> filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            List<String> excludedParams = List.of("page", "size", "sortBy", "sortDir");

            filters.forEach((field, value) -> {
                if (value != null && !value.isEmpty() && !excludedParams.contains(field)) {
                    try {
                        switch (field) {
                            case "medicalRecordCode":
                                predicates.add(cb.like(cb.lower(root.get("medicalRecordCode")), "%" + value.toLowerCase() + "%"));
                                break;
                            case "patientId":
                                predicates.add(cb.equal(root.get("patient").get("id"), value));
                                break;
                            case "createdById":
                                predicates.add(cb.equal(root.get("createdBy").get("id"), value));
                                break;
                            case "status":
                                try {
                                    MedicalRecordStatus status = MedicalRecordStatus.valueOf(value.toUpperCase());
                                    predicates.add(cb.equal(root.get("status"), status));
                                } catch (IllegalArgumentException ignored) {
                                }
                                break;
                            case "fromDate":
                                LocalDateTime from = LocalDateTime.parse(value);
                                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
                                break;
                            case "toDate":
                                LocalDateTime to = LocalDateTime.parse(value);
                                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
                                break;
                            default:
                                break;
                        }
                    } catch (Exception e) {
                        // Bỏ qua lỗi parse hoặc field không hợp lệ
                    }
                }
            });

            // Soft delete check
            predicates.add(cb.isNull(root.get("deletedAt")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
