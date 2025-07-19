package vn.edu.fpt.medicaldiagnosis.specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.fpt.medicaldiagnosis.entity.QueuePatients;
import vn.edu.fpt.medicaldiagnosis.enums.DepartmentType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QueuePatientsSpecification {

    public static Specification<QueuePatients> buildSpecification(Map<String, String> filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Object, Object> specializationJoin = root.join("specialization", JoinType.LEFT);

            for (Map.Entry<String, String> entry : filters.entrySet()) {
                String field = entry.getKey();
                String value = entry.getValue();

                if (value == null || value.isBlank()) continue;

                try {
                    switch (field) {
                        case "type":
                            predicates.add(cb.equal(root.get("type"), DepartmentType.valueOf(value.toUpperCase())));
                            break;
                        case "roomNumber":
                            predicates.add(cb.like(cb.lower(root.get("roomNumber")), "%" + value.toLowerCase() + "%"));
                            break;
                        case "status":
                            predicates.add(cb.equal(cb.lower(root.get("status")), value.toLowerCase()));
                            break;
                        case "registeredTime":
                            predicates.add(cb.like(cb.function("DATE_FORMAT", String.class,
                                    root.get("registeredTime"),
                                    cb.literal("%Y-%m-%d")), "%" + value + "%"));
                            break;
                        case "registeredTimeFrom":
                            predicates.add(cb.greaterThanOrEqualTo(root.get("registeredTime"), LocalDateTime.parse(value + "T00:00:00")));
                            break;
                        case "registeredTimeTo":
                            predicates.add(cb.lessThanOrEqualTo(root.get("registeredTime"), LocalDateTime.parse(value + "T23:59:59")));
                            break;
                        case "specialization":
                        case "specializationName":
                            predicates.add(cb.like(cb.lower(specializationJoin.get("name")), "%" + value.toLowerCase() + "%"));
                            break;
                    }
                } catch (Exception ignored) {}
            }

            predicates.add(cb.isNull(root.get("deletedAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
