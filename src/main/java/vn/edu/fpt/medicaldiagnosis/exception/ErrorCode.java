package vn.edu.fpt.medicaldiagnosis.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum ErrorCode {

    // ===== COMMON =====
    UNCATEGORIZED(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1000, "Invalid message key", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1001, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1002, "You don't have permission", HttpStatus.FORBIDDEN),
    VALIDATION_ERROR(1003, "Validation error", HttpStatus.BAD_REQUEST),

    // ===== ACCOUNT =====
    ACCOUNT_EXISTED(1101, "Account already exists", HttpStatus.BAD_REQUEST),
    ACCOUNT_NOT_FOUND(1102, "Account not found", HttpStatus.NOT_FOUND),
    ACCOUNT_USERNAME_REQUIRED(1103, "Username is required", HttpStatus.BAD_REQUEST),
    ACCOUNT_PASSWORD_INVALID(1104, "Password must be at least 8 characters", HttpStatus.BAD_REQUEST),
    ACCOUNT_ROLE_REQUIRED(1105, "Role is required", HttpStatus.BAD_REQUEST),

    // ===== PATIENT =====
    PATIENT_NOT_FOUND(1201, "Patient not found", HttpStatus.NOT_FOUND),
    PATIENT_NAME_REQUIRED(1202, "Full name is required", HttpStatus.BAD_REQUEST),
    PATIENT_CONTACT_REQUIRED(1203, "Contact info is required", HttpStatus.BAD_REQUEST),
    PATIENT_DOB_REQUIRED(1205, "Date of birth is required", HttpStatus.BAD_REQUEST),
    PATIENT_DOB_PAST(1206, "Date of birth must be in the past", HttpStatus.BAD_REQUEST),
    PATIENT_GENDER_REQUIRED(1207, "Gender is required", HttpStatus.BAD_REQUEST),
    PATIENT_FIRST_NAME_REQUIRED(1208, "First name is required", HttpStatus.BAD_REQUEST),
    PATIENT_LAST_NAME_REQUIRED(1209, "Last name is required", HttpStatus.BAD_REQUEST),
    PATIENT_PHONE_INVALID(1210, "Phone number must be 10 to 15 digits", HttpStatus.BAD_REQUEST),
    PATIENT_EMAIL_INVALID(1211, "Email format is invalid", HttpStatus.BAD_REQUEST),
    PATIENT_MIDDLE_NAME_REQUIRED(1212, "Middle name is required", HttpStatus.BAD_REQUEST),
    PATIENT_FIRST_NAME_TOO_LONG(1213, "First name must be at most 100 characters", HttpStatus.BAD_REQUEST),
    PATIENT_MIDDLE_NAME_TOO_LONG(1214, "Middle name must be at most 100 characters", HttpStatus.BAD_REQUEST),
    PATIENT_LAST_NAME_TOO_LONG(1215, "Last name must be at most 100 characters", HttpStatus.BAD_REQUEST),
    PATIENT_PHONE_REQUIRED(1216, "Phone number is required", HttpStatus.BAD_REQUEST),
    PATIENT_EMAIL_REQUIRED(1217, "Email is required", HttpStatus.BAD_REQUEST),
    PATIENT_EMAIL_EXISTED(1218, "Email already exists", HttpStatus.BAD_REQUEST),
    PATIENT_PHONE_EXISTED(1219, "Phone number already exists", HttpStatus.BAD_REQUEST),

    // ===== STAFF =====
    STAFF_NAME_EMPTY(1301, "Staff name cannot be empty", HttpStatus.BAD_REQUEST),
    STAFF_NAME_LENGTH(1302, "Staff name must be between 3 and 100 characters", HttpStatus.BAD_REQUEST),
    STAFF_SPECIALTY_EMPTY(1303, "Specialty cannot be empty", HttpStatus.BAD_REQUEST),
    STAFF_LEVEL_EMPTY(1304, "Level cannot be empty", HttpStatus.BAD_REQUEST),
    STAFF_PHONE_EMPTY(1305, "Phone number cannot be empty", HttpStatus.BAD_REQUEST),
    STAFF_PHONE_INVALID(1306, "Phone number must be 10 digits", HttpStatus.BAD_REQUEST),
    STAFF_EMAIL_EMPTY(1307, "Email cannot be empty", HttpStatus.BAD_REQUEST),
    STAFF_EMAIL_LENGTH(1308, "Email must be between 3 and 100 characters", HttpStatus.BAD_REQUEST),
    STAFF_EMAIL_INVALID(1309, "Invalid email format", HttpStatus.BAD_REQUEST),
    STAFF_GENDER_EMPTY(1310, "Gender cannot be empty", HttpStatus.BAD_REQUEST),
    STAFF_DOB_EMPTY(1311, "Date of birth cannot be empty", HttpStatus.BAD_REQUEST),
    STAFF_DOB_PAST(1312, "Date of birth must be in the past", HttpStatus.BAD_REQUEST),
    STAFF_EMAIL_EXISTED(1313, "Email already exists", HttpStatus.BAD_REQUEST),
    STAFF_PHONE_EXISTED(1314, "Phone number already exists", HttpStatus.BAD_REQUEST),
    STAFF_ACCOUNT_NOT_FOUND(1315, "Staff's account not found", HttpStatus.NOT_FOUND),
    STAFF_NOT_FOUND(1316, "Staff not found", HttpStatus.NOT_FOUND),
    STAFF_ACCOUNT_EXISTED(1317, "Account already exists", HttpStatus.BAD_REQUEST),
    STAFF_FIRST_NAME_REQUIRED(1318, "First name is required", HttpStatus.BAD_REQUEST),
    STAFF_FIRST_NAME_LENGTH(1318, "First name must be between 2 and 100 characters", HttpStatus.BAD_REQUEST),
    STAFF_LAST_NAME_REQUIRED(1319, "Last name is required", HttpStatus.BAD_REQUEST),
    STAFF_LAST_NAME_LENGTH(1320, "Last name must be between 2 and 100 characters", HttpStatus.BAD_REQUEST),
    STAFF_MIDDLE_NAME_LENGTH(1321, "Middle name must be between 2 and 100 characters", HttpStatus.BAD_REQUEST),

    // ===== DEPARTMENT =====
    DEPARTMENT_NAME_EMPTY(1401, "Department name cannot be empty", HttpStatus.BAD_REQUEST),
    DEPARTMENT_NAME_LENGTH(1402, "Department name must be between 3 and 100 characters", HttpStatus.BAD_REQUEST),
    DEPARTMENT_DESCRIPTION_LENGTH(1403, "Description must be between 3 and 500 characters", HttpStatus.BAD_REQUEST),
    DEPARTMENT_ROOM_EMPTY(1404, "Room number cannot be empty", HttpStatus.BAD_REQUEST),
    DEPARTMENT_ROOM_LENGTH(1405, "Room number must be between 2 and 5 characters", HttpStatus.BAD_REQUEST),
    DEPARTMENT_TYPE_EMPTY(1406, "Department Type cannot be null", HttpStatus.BAD_REQUEST),
    DEPARTMENT_NOT_FOUND(1407, "Department not found", HttpStatus.NOT_FOUND),
    DEPARTMENT_ROOM_EXISTED(1408, "Room number already exists", HttpStatus.BAD_REQUEST),

    // ===== SERVICE =====
    SERVICE_NAME_EMPTY(1501, "Service name cannot be empty", HttpStatus.BAD_REQUEST),
    SERVICE_NAME_LENGTH(1502, "Service name must be between 3 and 100 characters", HttpStatus.BAD_REQUEST),
    SERVICE_DESCRIPTION_LENGTH(1503, "Description must be between 3 and 500 characters", HttpStatus.BAD_REQUEST),
    PRICE_INVALID_FORMAT(400, "Price must be max 12 digits before decimal, max 3 digits after.", HttpStatus.BAD_REQUEST),
    PRICE_EMPTY(400, "Price cannot be empty", HttpStatus.BAD_REQUEST),
    PRICE_MIN_0(400, "Price must be at least 0", HttpStatus.BAD_REQUEST),
    DISCOUNT_MIN_0(400, "Discount must be at least 0", HttpStatus.BAD_REQUEST),
    DISCOUNT_MAX_100(400, "Discount must be at most 100", HttpStatus.BAD_REQUEST),
    DISCOUNT_INVALID_FORMAT(400, "Discount must 0.00 to 100.00", HttpStatus.BAD_REQUEST),
    VAT_EMPTY(400, "VAT cannot be empty", HttpStatus.BAD_REQUEST),
    VAT_INVALID_FORMAT(400, "VAT must be 0, 8 10", HttpStatus.BAD_REQUEST),
    DEPARTMENT_ID_EMPTY(400, "Department ID cannot be empty", HttpStatus.BAD_REQUEST),
    MEDICAL_SERVICE_ID_REQUIRED(400, "Department service ID cannot be empty", HttpStatus.BAD_REQUEST),
    MEDICAL_SERVICE_PRICE_NOT_FOUND(404, "Department service price not found", HttpStatus.NOT_FOUND),
    MEDICAL_SERVICE_NOT_FOUND(1504, "Service not found", HttpStatus.NOT_FOUND),
    SERVICE_CODE_EXISTED(1505, "Service code already existed", HttpStatus.BAD_REQUEST),

    // ===== TENANT =====
    TENANT_CODE_EXISTED(1501, "Tenant code already existed", HttpStatus.BAD_REQUEST),
    TENANT_NOT_FOUND(1502, "Tenant not found", HttpStatus.NOT_FOUND),

    // ===== ROLE & PERMISSION =====
    ROLE_NOT_FOUND(1601, "Role not found", HttpStatus.NOT_FOUND),
    PERMISSION_NOT_FOUND(1602, "Permission not found", HttpStatus.NOT_FOUND),
    ROLE_ALREADY_EXISTS(1603, "Role already exists", HttpStatus.CONFLICT),
    PERMISSION_ALREADY_EXISTS(1604, "Permission already exists", HttpStatus.CONFLICT),

    // ===== QUEUE PATIENT =====
    QUEUE_PATIENT_NOT_FOUND(1701, "QueuePatient not found", HttpStatus.NOT_FOUND),
    QUEUE_PATIENT_DUPLICATE_WAITING(1702, "Patient is already in a waiting queue", HttpStatus.BAD_REQUEST),
    QUEUE_PATIENT_INVALID_STATUS(1703, "QueuePatient status is invalid", HttpStatus.BAD_REQUEST),
    QUEUE_PATIENT_ALREADY_CHECKED_OUT(1704, "QueuePatient already checked out", HttpStatus.BAD_REQUEST),
    QUEUE_NOT_FOUND(1705, "Queue not found", HttpStatus.NOT_FOUND),
    QUEUE_PATIENT_ALREADY_FINISHED(1706, "QueuePatient already finished", HttpStatus.BAD_REQUEST),
    // ===== INVOICE =====
    INVOICE_NOT_FOUND(1801, "Invoice not found", HttpStatus.NOT_FOUND),
    INVOICE_STATUS_INVALID(1802, "Invoice status is invalid", HttpStatus.BAD_REQUEST),
    INVOICE_PATIENT_NOT_FOUND(1803, "Patient not found", HttpStatus.NOT_FOUND),
    INVOICE_ALREADY_PAID(1804, "Invoice is already paid", HttpStatus.BAD_REQUEST),
    INVOICE_ALREADY_CANCELLED(1805, "Invoice is already cancelled", HttpStatus.BAD_REQUEST),
    INVOICE_ID_EMPTY(1806, "Invoice ID cannot be empty", HttpStatus.BAD_REQUEST),
    PAYMENT_TYPE_INVALID(1808, "Payment type is invalid", HttpStatus.BAD_REQUEST),
    ;

    private int code;
    private String message;
    private HttpStatusCode statusCode;
}
