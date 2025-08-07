package vn.edu.fpt.medicaldiagnosis.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.dto.request.MedicalServiceFeedbackRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.MedicalServiceFeedbackResponse;
import vn.edu.fpt.medicaldiagnosis.entity.*;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.repository.*;
import vn.edu.fpt.medicaldiagnosis.service.MedicalServiceFeedbackService;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class MedicalServiceFeedbackServiceImpl implements MedicalServiceFeedbackService {

    MedicalServiceFeedbackRepository repository;
    MedicalServiceRepository medicalServiceRepository;
    PatientRepository patientRepository;
    MedicalRecordRepository medicalRecordRepository;

    @Override
    public MedicalServiceFeedbackResponse create(MedicalServiceFeedbackRequest request) {
        log.info("Creating medical service feedback: {}", request);

        MedicalService service = medicalServiceRepository.findByIdAndDeletedAtIsNull(request.getMedicalServiceId())
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_SERVICE_NOT_FOUND, "Service with ID " + request.getMedicalServiceId() + " not found"));

        Patient patient = patientRepository.findByIdAndDeletedAtIsNull(request.getPatientId())
                .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND, "Patient with ID " + request.getPatientId() + " not found"));

        MedicalRecord order = medicalRecordRepository.findByIdAndDeletedAtIsNull(request.getMedicalRecordId())
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_ORDER_NOT_FOUND, "Order with ID " + request.getMedicalRecordId() + " not found"));

        MedicalServiceFeedback feedback = MedicalServiceFeedback.builder()
                .medicalService(service)
                .patient(patient)
                .medicalRecord(order)
                .satisfactionLevel(request.getSatisfactionLevel())
                .comment(request.getComment())
                .build();

        return mapToResponse(repository.save(feedback));
    }

    @Override
    public MedicalServiceFeedbackResponse update(String id, MedicalServiceFeedbackRequest request) {
        MedicalServiceFeedback feedback = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.FEEDBACK_NOT_FOUND, "Feedback with ID " + id + " not found"));

        MedicalService service = medicalServiceRepository.findByIdAndDeletedAtIsNull(request.getMedicalServiceId())
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_SERVICE_NOT_FOUND, "Service with ID " + request.getMedicalServiceId() + " not found"));

        Patient patient = patientRepository.findByIdAndDeletedAtIsNull(request.getPatientId())
                .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND, "Patient with ID " + request.getPatientId() + " not found"));

        MedicalRecord record = medicalRecordRepository.findByIdAndDeletedAtIsNull(request.getMedicalRecordId())
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_ORDER_NOT_FOUND, "Order with ID " + request.getMedicalRecordId() + " not found"));

        feedback.setMedicalService(service);
        feedback.setPatient(patient);
        feedback.setMedicalRecord(record);
        feedback.setSatisfactionLevel(request.getSatisfactionLevel());
        feedback.setComment(request.getComment());

        return mapToResponse(repository.save(feedback));
    }

    @Override
    public List<MedicalServiceFeedbackResponse> findAll() {
        return repository.findAllByDeletedAtIsNull().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public MedicalServiceFeedbackResponse findById(String id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new AppException(ErrorCode.FEEDBACK_NOT_FOUND, "Feedback with ID " + id + " not found"));
    }

    @Override
    public void delete(String id) {
        MedicalServiceFeedback feedback = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.FEEDBACK_NOT_FOUND, "Feedback with ID " + id + " not found"));
        repository.delete(feedback);
    }

    @Override
    public List<MedicalServiceFeedbackResponse> findByMedicalRecordId(String medicalRecordId) {
        List<MedicalServiceFeedback> feedbacks = repository
                .findAllByMedicalRecordIdAndDeletedAtIsNull(medicalRecordId);

        return feedbacks.stream()
                .map(this::mapToResponse)
                .toList();
    }


    private MedicalServiceFeedbackResponse mapToResponse(MedicalServiceFeedback feedback) {
        return MedicalServiceFeedbackResponse.builder()
                .id(feedback.getId())
                .medicalServiceId(feedback.getMedicalService().getId())
                .medicalServiceName(feedback.getMedicalService().getName()) // assuming getName() exists
                .patientId(feedback.getPatient().getId())
                .patientName(feedback.getPatient().getFullName()) // assuming getFullName() exists
                .medicalRecordId(feedback.getMedicalRecord().getId())
                .satisfactionLevel(feedback.getSatisfactionLevel())
                .comment(feedback.getComment())
                .createdAt(feedback.getCreatedAt())
                .build();
    }
}

