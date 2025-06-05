package vn.edu.fpt.medicaldiagnosis.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.dto.request.StaffCreateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.StaffUpdateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.StaffResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Staff;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.StaffMapper;
import vn.edu.fpt.medicaldiagnosis.repository.StaffRepository;
import vn.edu.fpt.medicaldiagnosis.service.StaffService;
import vn.edu.fpt.medicaldiagnosis.specification.StaffSpecification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class StaffServiceImpl implements StaffService {
    StaffRepository staffRepository;
    StaffMapper staffMapper;
    @Override
    public StaffResponse createStaff(StaffCreateRequest staffCreateRequest) {
        log.info("Service: create staff");
        log.info("Creating staff: {}", staffCreateRequest);

        if (staffRepository.existsByEmailAndDeletedAtIsNull(staffCreateRequest.getEmail())) {
            log.info("Email already exists.");
            throw new AppException(ErrorCode.STAFF_EMAIL_EXISTED);
        }

        if (staffRepository.existsByPhoneAndDeletedAtIsNull(staffCreateRequest.getPhone())) {
            log.info("Phone number already exists.");
            throw new AppException(ErrorCode.STAFF_PHONE_EXISTED);
        }
        Staff staff = staffMapper.toStaff(staffCreateRequest);

        staff = staffRepository.save(staff);

        log.info("staff created: {}", staff);

        return staffMapper.toStaffResponse(staff);
    }

    @Override
    public List<StaffResponse> getAllStaffs() {
        log.info("Service: get all staffs");
        List<Staff> staffs = staffRepository.findAllByDeletedAtIsNull();
        return staffs.stream().map(staffMapper::toStaffResponse).collect(Collectors.toList());
    }

    @Override
    public StaffResponse getStaffById(String id) {
        log.info("Service: get staff by id: {}", id);
        return staffMapper.toStaffResponse(
                staffRepository.findByIdAndDeletedAtIsNull(id).orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND)));
    }

    @Override
    public void deleteStaff(String id) {
        log.info("Service: delete staff {}", id);
        Staff staff = staffRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));
        staff.setDeletedAt(LocalDateTime.now());
        staffRepository.save(staff);
    }

    @Override
    public StaffResponse updateStaff(String id, StaffUpdateRequest staffUpdateRequest) {
        log.info("Service: update staff {}", id);

        Staff staff = staffRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        if (staffRepository.existsByEmailAndDeletedAtIsNullAndIdNot(staffUpdateRequest.getEmail(), id)) {
            log.info("Email already exists.");
            throw new AppException(ErrorCode.STAFF_EMAIL_EXISTED);
        }

        if (staffRepository.existsByPhoneAndDeletedAtIsNullAndIdNot(staffUpdateRequest.getPhone(), id)) {
            log.info("Phone number already exists.");
            throw new AppException(ErrorCode.STAFF_PHONE_EXISTED);
        }

        staffMapper.updateStaff(staff, staffUpdateRequest);
        log.info("Staff: {}", staff);
        return staffMapper.toStaffResponse(staffRepository.save(staff));
    }

    @Override
    public Page<StaffResponse> getStaffsPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Staff> spec = StaffSpecification.buildSpecification(filters);

        Page<Staff> pageResult = staffRepository.findAll(spec, pageable);
        return pageResult.map(staffMapper::toStaffResponse);
    }
}
