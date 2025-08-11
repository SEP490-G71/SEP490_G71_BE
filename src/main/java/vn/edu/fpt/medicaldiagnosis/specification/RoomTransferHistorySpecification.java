package vn.edu.fpt.medicaldiagnosis.specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.fpt.medicaldiagnosis.common.DataUtil;
import vn.edu.fpt.medicaldiagnosis.entity.Department;
import vn.edu.fpt.medicaldiagnosis.entity.MedicalRecord;
import vn.edu.fpt.medicaldiagnosis.entity.RoomTransferHistory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RoomTransferHistorySpecification {
    public static Specification<RoomTransferHistory> build(Map<String, String> filters) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();

            // join các bảng cần tìm
            Join<RoomTransferHistory, MedicalRecord> recordJoin = root.join("medicalRecord", JoinType.LEFT);
            Join<RoomTransferHistory, Department> fromDeptJoin  = root.join("fromDepartment", JoinType.INNER);
            Join<RoomTransferHistory, Department> toDeptJoin    = root.join("toDepartment", JoinType.INNER);

            // ====== Filters ======
            String fromDateStr       = filters.get("fromDate");       // yyyy-MM-dd
            String toDateStr         = filters.get("toDate");         // yyyy-MM-dd
            String toDepartmentId    = filters.get("toDepartmentId");
            String medicalRecordCode = filters.get("medicalRecordCode");

            // Range by transferTime
            if (fromDateStr != null && !fromDateStr.isBlank()) {
                LocalDate d = LocalDate.parse(fromDateStr);
                ps.add(cb.greaterThanOrEqualTo(root.get("transferTime"), d.atStartOfDay()));
            }
            if (toDateStr != null && !toDateStr.isBlank()) {
                LocalDate d = LocalDate.parse(toDateStr);
                ps.add(cb.lessThanOrEqualTo(root.get("transferTime"), d.atTime(23,59,59)));
            }

            // toDepartmentId
            if (toDepartmentId != null && !toDepartmentId.isBlank()) {
                ps.add(cb.equal(toDeptJoin.get("id"), toDepartmentId));
            }

            // medicalRecordCode (LIKE, case-insensitive)
            if (medicalRecordCode != null && !medicalRecordCode.isBlank()) {
                String norm = DataUtil.normalizeForSearch(medicalRecordCode);
                ps.add(cb.like(cb.lower(recordJoin.get("medicalRecordCode")), "%" + norm + "%"));
            }

            ps.add(cb.notEqual(fromDeptJoin.get("id"), toDeptJoin.get("id")));

            return cb.and(ps.toArray(new Predicate[0]));
        };
    }
}
