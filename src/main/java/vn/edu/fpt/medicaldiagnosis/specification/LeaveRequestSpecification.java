package vn.edu.fpt.medicaldiagnosis.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.fpt.medicaldiagnosis.entity.LeaveRequest;
import vn.edu.fpt.medicaldiagnosis.enums.LeaveRequestStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LeaveRequestSpecification {
    public static Specification<LeaveRequest> buildSpecification(Map<String, String> filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            List<String> excludedParams = List.of("page", "size", "sortBy", "sortDir");

            filters.forEach((field, value) -> {
                if (value != null && !value.isEmpty() && !excludedParams.contains(field)) {
                    switch (field) {
                        case "staffId":
                            predicates.add(cb.equal(root.get("staff").get("id"), value));
                            break;
                        case "status":
                            try {
                                LeaveRequestStatus status = LeaveRequestStatus.valueOf(value.toUpperCase());
                                predicates.add(cb.equal(root.get("status"), status));
                            } catch (IllegalArgumentException ignored) {}
                            break;
                        case "createdAtFrom":
                            LocalDate fromDate = LocalDate.parse(value);
                            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt").as(LocalDate.class), fromDate));
                            break;
                        case "createdAtTo":
                            LocalDate toDate = LocalDate.parse(value);
                            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt").as(LocalDate.class), toDate));
                            break;
                        default:
                            break;
                    }
                }
            });

            predicates.add(cb.isNull(root.get("deletedAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
