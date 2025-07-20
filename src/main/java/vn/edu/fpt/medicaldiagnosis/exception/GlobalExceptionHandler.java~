package vn.edu.fpt.medicaldiagnosis.exception;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.validation.ConstraintViolation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    private static final String MIN_ATTRIBUTE = "min";

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ApiResponse<String>> handlingRuntimeException(RuntimeException ex) {
        log.info("Exception: " + ex);
        ErrorCode errorCode = ErrorCode.UNCATEGORIZED;

        return ResponseEntity.status(errorCode.getStatusCode())
                .body(ApiResponse.<String>builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    @ExceptionHandler(value = AppException.class)
    public ResponseEntity<ApiResponse<String>> handlingAppException(AppException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        log.info("ErrorCode: " + errorCode);
        return ResponseEntity.status(errorCode.getStatusCode())
                .body(ApiResponse.<String>builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    public ResponseEntity<ApiResponse<String>> handlingAccessDeniedException(AccessDeniedException ex) {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;

        return ResponseEntity.status(errorCode.getStatusCode())
                .body(ApiResponse.<String>builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<ValidationError>>> handleValidationException(MethodArgumentNotValidException ex) {
        List<ValidationError> validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> {
                    String errorCodeStr = error.getDefaultMessage();
                    ErrorCode errorCode;
                    try {
                        errorCode = ErrorCode.valueOf(errorCodeStr);
                    } catch (IllegalArgumentException e) {
                        errorCode = ErrorCode.INVALID_KEY;
                    }

                    Map<String, Object> attributes = new HashMap<>();
                    try {
                        ConstraintViolation<?> constraintViolation =
                                error.unwrap(ConstraintViolation.class);
                        attributes = constraintViolation.getConstraintDescriptor().getAttributes();
                    } catch (Exception ignored) {}

                    return ValidationError.builder()
                            .field(error.getField())
                            .message(mapAttribute(errorCode.getMessage(), attributes))
                            .build();
                })
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse.<List<ValidationError>>builder()
                        .code(ErrorCode.VALIDATION_ERROR.getCode())
                        .message(ErrorCode.VALIDATION_ERROR.getMessage())
                        .result(validationErrors)
                        .build()
        );
    }


    private String mapAttribute(String message, Map<String, Object> attributes) {
        String minValue = String.valueOf(attributes.get(MIN_ATTRIBUTE));
        if (!Objects.isNull(minValue)) {
            message = message.replace("{" + MIN_ATTRIBUTE + "}", minValue);
        }
        return message;
    }
}
