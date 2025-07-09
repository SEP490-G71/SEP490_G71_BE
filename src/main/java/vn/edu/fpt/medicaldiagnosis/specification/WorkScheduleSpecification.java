package vn.edu.fpt.medicaldiagnosis.specification;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.fpt.medicaldiagnosis.entity.WorkSchedule;
import vn.edu.fpt.medicaldiagnosis.enums.Shift;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WorkScheduleSpecification {
    public static Specification<WorkSchedule> buildSpecification(Map<String, String> filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            List<String> excluded = List.of("page", "size", "sortBy", "sortDir");

            filters.forEach((key, value) -> {
                if (value != null && !value.isEmpty() && !excluded.contains(key)) {
                    switch (key) {
                        case "staffName":
                            predicates.add(cb.like(cb.lower(root.get("staff").get("fullName")), "%" + value.toLowerCase() + "%"));
                            break;
                        case "shift":
                            try {
                                Shift shift = Shift.valueOf(value.toUpperCase());
                                predicates.add(cb.equal(root.get("shift"), shift));
                            } catch (IllegalArgumentException ignored) {}
                            break;
                        case "fromDate":
                            predicates.add(cb.greaterThanOrEqualTo(root.get("shiftDate"), LocalDate.parse(value)));
                            break;
                        case "toDate":
                            predicates.add(cb.lessThanOrEqualTo(root.get("shiftDate"), LocalDate.parse(value)));
                            break;
                        case "dayOfWeek": // NEW: lọc theo thứ
                            try {
                                DayOfWeek day = DayOfWeek.valueOf(value.toUpperCase());
                                Expression<Integer> dayExpr = cb.function("DAYOFWEEK", Integer.class, root.get("shiftDate"));
                                predicates.add(cb.equal(dayExpr, day.getValue() + 1)); // Java: MONDAY=1, SQL: SUNDAY=1
                            } catch (IllegalArgumentException ignored) {}
                            break;
                    }
                }
            });

            predicates.add(cb.isNull(root.get("deletedAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
