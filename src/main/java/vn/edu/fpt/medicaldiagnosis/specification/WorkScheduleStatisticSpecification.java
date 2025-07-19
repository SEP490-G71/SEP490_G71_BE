package vn.edu.fpt.medicaldiagnosis.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.fpt.medicaldiagnosis.entity.WorkSchedule;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WorkScheduleStatisticSpecification {
    public static Specification<WorkSchedule> buildSpecification(Map<String, String> filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            List<String> excluded = List.of("page", "size", "sortBy", "sortDir");

            filters.forEach((key, value) -> {
                if (value != null && !value.isEmpty() && !excluded.contains(key)) {
                    switch (key) {
                        case "staffId":
                            predicates.add(cb.equal(root.get("staff").get("id"), value)); // hoặc UUID.fromString(value) nếu id là UUID
                            break;
                        case "fromDate":
                            predicates.add(cb.greaterThanOrEqualTo(root.get("shiftDate"), LocalDate.parse(value)));
                            break;
                        case "toDate":
                            predicates.add(cb.lessThanOrEqualTo(root.get("shiftDate"), LocalDate.parse(value)));
                            break;
                    }
                }
            });

            predicates.add(cb.isNull(root.get("deletedAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
