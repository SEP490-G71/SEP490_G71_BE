package vn.edu.fpt.medicaldiagnosis.specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.fpt.medicaldiagnosis.entity.QueuePatients;
import vn.edu.fpt.medicaldiagnosis.enums.DepartmentType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class QueuePatientsSpecification {

    public static Specification<QueuePatients> buildSpecification(Map<String, String> filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            List<String> excludedParams = List.of("page", "size", "sortBy", "sortDir");

            Join<Object, Object> specializationJoin = root.join("specialization", JoinType.LEFT);

            for (Map.Entry<String, String> entry : filters.entrySet()) {
                String field = entry.getKey();
                String value = entry.getValue();

                log.info("Field: {}, Value: {}", field, value);

                if (value == null || value.isBlank() || excludedParams.contains(field)) continue;

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
                            predicates.add(cb.greaterThanOrEqualTo(
                                    root.get("registeredTime"), LocalDateTime.parse(value)
                            ));
                            break;
                        case "registeredTimeTo":
                            predicates.add(cb.lessThan(
                                    root.get("registeredTime"), LocalDateTime.parse(value).plusDays(1)
                            ));
                            break;
                        case "specialization":
                            predicates.add(cb.like(cb.lower(specializationJoin.get("name")), "%" + value.toLowerCase() + "%"));
                            break;
                        default:
                            // fallback nếu có key khớp với attribute của QueuePatients
                            if (root.getModel().getAttributes().stream().anyMatch(a -> a.getName().equals(field))) {
                                predicates.add(cb.like(cb.lower(root.get(field)), "%" + value.toLowerCase() + "%"));
                            }
                    }
                } catch (Exception ignored) {
                }
            }

            predicates.add(cb.isNull(root.get("deletedAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
