package vn.edu.fpt.medicaldiagnosis.specification;

import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.fpt.medicaldiagnosis.common.DataUtil;
import vn.edu.fpt.medicaldiagnosis.entity.RegisteredOnline;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class RegisteredOnlineSpecification {

    public static Specification<RegisteredOnline> buildSpecification(Map<String, String> filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            List<String> excludedParams = List.of("page", "size", "sortBy", "sortDir");

            filters.forEach((field, value) -> {
                if (value != null && !value.isEmpty() && !excludedParams.contains(field)) {
                    String normalizedValue = DataUtil.normalizeForSearch(value);

                    try {
                        switch (field) {
                            case "fullName":
                            case "email":
                            case "phoneNumber":
                            case "message":
                                predicates.add(cb.like(cb.lower(root.get(field)), "%" + normalizedValue + "%"));
                                break;
                            case "registeredAt":
                                try {
                                    // Parse từ chuỗi thành LocalDate
                                    LocalDate targetDate = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
                                    // So sánh bằng cách extract date từ LocalDateTime
                                    predicates.add(cb.equal(cb.function("DATE", LocalDate.class, root.get("registeredAt")), targetDate));
                                } catch (DateTimeParseException e) {
                                    // Bỏ qua nếu format sai
                                }
                                break;
                            case "isConfirmed":
                                // Parse boolean từ chuỗi
                                Boolean confirmed = Boolean.parseBoolean(value);
                                log.info("Confirmed: {}", confirmed);
                                predicates.add(cb.equal(root.get("isConfirmed"), confirmed));
                                break;
                            default:
                                break;
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            });

            predicates.add(cb.isNull(root.get("deletedAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
