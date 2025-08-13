package vn.edu.fpt.medicaldiagnosis.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.fpt.medicaldiagnosis.entity.MetricAlert;
import vn.edu.fpt.medicaldiagnosis.enums.AlertLevel;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MetricAlertSpecification {
    public static Specification<MetricAlert> buildSpecification(Map<String, String> filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            List<String> excluded = List.of("page", "size", "sortBy", "sortDir");

            // always exclude soft-deleted
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (filters == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            String from = filters.get("fromDate");
            String to   = filters.get("toDate");
            String levelStr = filters.get("level");
            String metricCode = filters.get("metricCode");
            String resolvedStr = filters.get("resolved");

            // date range: áp vào period_start/period_end (giao nhau với [from,to])
            // Nếu bạn muốn lọc theo "nằm trọn trong khoảng", sửa lại theo nhu cầu.
            try {
                if (from != null && !from.isBlank()) {
                    LocalDate fromDate = LocalDate.parse(from);
                    // period_end >= from
                    predicates.add(cb.greaterThanOrEqualTo(root.get("periodEnd"), fromDate));
                }
            } catch (Exception ignored) {}
            try {
                if (to != null && !to.isBlank()) {
                    LocalDate toDate = LocalDate.parse(to);
                    // period_start <= to
                    predicates.add(cb.lessThanOrEqualTo(root.get("periodStart"), toDate));
                }
            } catch (Exception ignored) {}

            // level
            if (levelStr != null && !levelStr.isBlank()) {
                try {
                    AlertLevel level = AlertLevel.valueOf(levelStr.toUpperCase());
                    predicates.add(cb.equal(root.get("level"), level));
                } catch (Exception ignored) {}
            }

            // metricCode (optional)
            if (metricCode != null && !metricCode.isBlank()) {
                predicates.add(cb.equal(root.get("metricCode"), metricCode));
            }

            // resolved (optional)
            if (resolvedStr != null && !resolvedStr.isBlank()) {
                if ("true".equalsIgnoreCase(resolvedStr) || "false".equalsIgnoreCase(resolvedStr)) {
                    predicates.add(cb.equal(root.get("resolved"), Boolean.valueOf(resolvedStr)));
                }
            }

            // các key khác nếu cần mở rộng sau: reason contains, diffPct >= X, ...

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
