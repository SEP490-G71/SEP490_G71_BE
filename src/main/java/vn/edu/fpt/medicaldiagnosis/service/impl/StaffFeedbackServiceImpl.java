package vn.edu.fpt.medicaldiagnosis.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.dto.request.StaffFeedbackRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.StaffFeedbackResponse;
import vn.edu.fpt.medicaldiagnosis.entity.MedicalRecord;
import vn.edu.fpt.medicaldiagnosis.entity.Patient;
import vn.edu.fpt.medicaldiagnosis.entity.Staff;
import vn.edu.fpt.medicaldiagnosis.entity.StaffFeedback;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.repository.MedicalRecordRepository;
import vn.edu.fpt.medicaldiagnosis.repository.PatientRepository;
import vn.edu.fpt.medicaldiagnosis.repository.StaffFeedbackRepository;
import vn.edu.fpt.medicaldiagnosis.repository.StaffRepository;
import vn.edu.fpt.medicaldiagnosis.service.StaffFeedbackService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class StaffFeedbackServiceImpl implements StaffFeedbackService {
    StaffFeedbackRepository repository;
    StaffRepository staffRepository;
    PatientRepository patientRepository;
    MedicalRecordRepository medicalRecordRepository;

    @Override
    public StaffFeedbackResponse create(StaffFeedbackRequest request) {
        log.info("Creating staff feedback: {}", request);
        Staff doctor = staffRepository.findByIdAndDeletedAtIsNull(request.getDoctorId())
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND, "Doctor with ID " + request.getDoctorId() + " not found"));

        Patient patient = patientRepository.findByIdAndDeletedAtIsNull(request.getPatientId())
                .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND, "Patient with ID " + request.getPatientId() + " not found"));

        MedicalRecord record = medicalRecordRepository.findByIdAndDeletedAtIsNull(request.getMedicalRecordId())
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND, "Medical record with ID " + request.getMedicalRecordId() + " not found"));

        StaffFeedback feedback = StaffFeedback.builder()
                .doctor(doctor)
                .patient(patient)
                .medicalRecord(record)
                .satisfactionLevel(request.getSatisfactionLevel())
                .comment(request.getComment())
                .build();

        return mapToResponse(repository.save(feedback));
    }

    @Override
    public StaffFeedbackResponse update(String id, StaffFeedbackRequest request) {
        StaffFeedback feedback = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.FEEDBACK_NOT_FOUND, "Feedback with ID " + id + " not found"));

        Staff doctor = staffRepository.findByIdAndDeletedAtIsNull(request.getDoctorId())
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND, "Doctor with ID " + request.getDoctorId() + " not found"));

        Patient patient = patientRepository.findByIdAndDeletedAtIsNull(request.getPatientId())
                .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND, "Patient with ID " + request.getPatientId() + " not found"));

        MedicalRecord record = medicalRecordRepository.findByIdAndDeletedAtIsNull(request.getMedicalRecordId())
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND, "Medical record with ID " + request.getMedicalRecordId() + " not found"));

        feedback.setDoctor(doctor);
        feedback.setPatient(patient);
        feedback.setMedicalRecord(record);
        feedback.setSatisfactionLevel(request.getSatisfactionLevel());
        feedback.setComment(request.getComment());

        return mapToResponse(repository.save(feedback));
    }

    @Override
    public List<StaffFeedbackResponse> findAll() {
        return repository.findAllByDeletedAtIsNull().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public StaffFeedbackResponse findById(String id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new AppException(ErrorCode.FEEDBACK_NOT_FOUND, "Feedback with ID " + id + " not found"));
    }

    @Override
    public void delete(String id) {
        StaffFeedback feedback = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.FEEDBACK_NOT_FOUND, "Feedback with ID " + id + " not found"));
        repository.delete(feedback);
    }

    private StaffFeedbackResponse mapToResponse(StaffFeedback feedback) {
        return StaffFeedbackResponse.builder()
                .id(feedback.getId())
                .doctorId(feedback.getDoctor().getId())
                .doctorName(feedback.getDoctor().getFullName()) // assuming getFullName exists
                .patientId(feedback.getPatient().getId())
                .patientName(feedback.getPatient().getFullName()) // assuming getFullName exists
                .medicalRecordId(feedback.getMedicalRecord().getId())
                .satisfactionLevel(feedback.getSatisfactionLevel())
                .comment(feedback.getComment())
                .createdAt(feedback.getCreatedAt())
                .build();
    }
}
