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
    INVALID_NEW_PASSWORD(1106, "The new password must not be the same as the old password.", HttpStatus.BAD_REQUEST),
    // ===== FORGET PASSWORD =====
    FORGET_PASSWORD_USERNAME_REQUIRED(1110, "Username is required", HttpStatus.BAD_REQUEST),
    FORGET_PASSWORD_OLD_PASSWORD_REQUIRED(1111, "Old password is required", HttpStatus.BAD_REQUEST),
    FORGET_PASSWORD_OLD_PASSWORD_INVALID(1112, "Old password must be at least 8 characters", HttpStatus.BAD_REQUEST),
    FORGET_PASSWORD_NEW_PASSWORD_REQUIRED(1113, "New password is required", HttpStatus.BAD_REQUEST),
    FORGET_PASSWORD_NEW_PASSWORD_INVALID(1114, "New password must be at least 8 characters", HttpStatus.BAD_REQUEST),
    ACCOUNT_OR_PASSWORD_INVALID(1115, "Account or password invalid", HttpStatus.BAD_REQUEST),

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
    PATIENT_ID_REQUIRED(1220, "Patient id is required", HttpStatus.BAD_REQUEST),

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
    STAFF_MIDDLE_NAME_LENGTH(1321, "Middle name must be less than 100 characters", HttpStatus.BAD_REQUEST),
    STAFF_ROLE_NAMES_EMPTY(1322, "Role names cannot be empty", HttpStatus.BAD_REQUEST),

    // ===== DEPARTMENT =====
    DEPARTMENT_NAME_EMPTY(1401, "Department name cannot be empty", HttpStatus.BAD_REQUEST),
    DEPARTMENT_NAME_LENGTH(1402, "Department name must be between 3 and 100 characters", HttpStatus.BAD_REQUEST),
    DEPARTMENT_DESCRIPTION_LENGTH(1403, "Description must be between 3 and 500 characters", HttpStatus.BAD_REQUEST),
    DEPARTMENT_ROOM_EMPTY(1404, "Room number cannot be empty", HttpStatus.BAD_REQUEST),
    DEPARTMENT_ROOM_LENGTH(1405, "Room number must be between 2 and 5 characters", HttpStatus.BAD_REQUEST),
    DEPARTMENT_TYPE_EMPTY(1406, "Department Type cannot be null", HttpStatus.BAD_REQUEST),
    DEPARTMENT_NOT_FOUND(1407, "Department not found", HttpStatus.NOT_FOUND),
    DEPARTMENT_ROOM_EXISTED(1408, "Room number already exists", HttpStatus.BAD_REQUEST),
    INVALID_ROOM_FOR_DEPARTMENT(1409, "The selected room is not valid for the specified department", HttpStatus.BAD_REQUEST),
    DEPARTMENT_TYPE_NAME_EMPTY(1410, "Department type name cannot be empty", HttpStatus.BAD_REQUEST),
    DEPARTMENT_TYPE_NAME_LENGTH(1411, "Department type name must be between 3 and 100 characters", HttpStatus.BAD_REQUEST),
    DEPARTMENT_TYPE_DESCRIPTION_LENGTH(1412, "Description must be between 3 and 500 characters", HttpStatus.BAD_REQUEST),
    DEPARTMENT_TYPE_NAME_EXISTED(1413, "Department type name already exists", HttpStatus.BAD_REQUEST),
    DEPARTMENT_TYPE_NOT_FOUND(1414, "Department type not found", HttpStatus.NOT_FOUND),
    DEPARTMENT_TYPE_ID_EMPTY(1415, "Department type id cannot be empty", HttpStatus.BAD_REQUEST),

    // ===== SERVICE =====
    SERVICE_NAME_EMPTY(1501, "Service name cannot be empty", HttpStatus.BAD_REQUEST),
    SERVICE_NAME_LENGTH(1502, "Service name must be between 3 and 100 characters", HttpStatus.BAD_REQUEST),
    SERVICE_DESCRIPTION_LENGTH(1503, "Description must be between 3 and 500 characters", HttpStatus.BAD_REQUEST),
    MEDICAL_SERVICE_NOT_FOUND(1504, "Service not found", HttpStatus.NOT_FOUND),
    SERVICE_CODE_EXISTED(1505, "Service code already existed", HttpStatus.BAD_REQUEST),
    PRICE_INVALID_FORMAT(1506, "Price must be max 12 digits before decimal, max 3 digits after.", HttpStatus.BAD_REQUEST),
    PRICE_EMPTY(1507, "Price cannot be empty", HttpStatus.BAD_REQUEST),
    PRICE_MIN_0(1508, "Price must be at least 0", HttpStatus.BAD_REQUEST),
    DISCOUNT_MIN_0(1509, "Discount must be at least 0", HttpStatus.BAD_REQUEST),
    DISCOUNT_MAX_100(1510, "Discount must be at most 100", HttpStatus.BAD_REQUEST),
    DISCOUNT_INVALID_FORMAT(1511, "Discount must be 0.00 to 100.00", HttpStatus.BAD_REQUEST),
    VAT_EMPTY(1512, "VAT cannot be empty", HttpStatus.BAD_REQUEST),
    VAT_INVALID_FORMAT(1513, "VAT must be 0, 8 or 10", HttpStatus.BAD_REQUEST),
    DEPARTMENT_ID_EMPTY(1514, "Department ID cannot be empty", HttpStatus.BAD_REQUEST),
    MEDICAL_SERVICE_ID_REQUIRED(1515, "Department service ID cannot be empty", HttpStatus.BAD_REQUEST),
    MEDICAL_SERVICE_PRICE_NOT_FOUND(1516, "Department service price not found", HttpStatus.NOT_FOUND),

    // ===== TENANT =====
    TENANT_CODE_EXISTED(3001, "Tenant code already existed", HttpStatus.BAD_REQUEST),
    TENANT_NOT_FOUND(3002, "Tenant not found", HttpStatus.NOT_FOUND),

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
    QUEUE_ORDER_CONFLICT(1707, "Queue order conflict", HttpStatus.BAD_REQUEST),
    ALREADY_IN_QUEUE(1708, "Patient has already been in a progress or waiting status", HttpStatus.BAD_REQUEST),
    REGISTERED_TIME_REQUIRED(1709, "Registered time cannot be empty", HttpStatus.BAD_REQUEST),
    INVALID_QUEUE_DATE(1710, "Queue date must not be in the past", HttpStatus.BAD_REQUEST),

    // ===== INVOICE =====
    INVOICE_NOT_FOUND(1801, "Invoice not found", HttpStatus.NOT_FOUND),
    INVOICE_STATUS_INVALID(1802, "Invoice status is invalid", HttpStatus.BAD_REQUEST),
    INVOICE_PATIENT_NOT_FOUND(1803, "Patient not found", HttpStatus.NOT_FOUND),
    INVOICE_ALREADY_PAID(1804, "Invoice is already paid", HttpStatus.BAD_REQUEST),
    INVOICE_ALREADY_CANCELLED(1805, "Invoice is already cancelled", HttpStatus.BAD_REQUEST),
    INVOICE_ID_EMPTY(1806, "Invoice ID cannot be empty", HttpStatus.BAD_REQUEST),
    PAYMENT_TYPE_INVALID(1808, "Payment type is invalid", HttpStatus.BAD_REQUEST),
    MEDICAL_RECORD_NOT_FOUND(1809, "Medical record not found", HttpStatus.NOT_FOUND),
    MULTIPLE_MEDICAL_RECORDS_FOUND(1810, "Multiple medical records found", HttpStatus.BAD_REQUEST),
    MEDICAL_ORDER_NOT_FOUND(1811, "Medical order not found", HttpStatus.NOT_FOUND),
    INVOICE_PDF_CREATION_FAILED(1812, "Failed to create invoice PDF", HttpStatus.INTERNAL_SERVER_ERROR),
    PAYMENT_REQUIRED(1813, "Payment is required", HttpStatus.BAD_REQUEST),

    // ===== file =====
    FILE_NOT_PROVIDED(1901, "File not provided", HttpStatus.BAD_REQUEST),
    FILE_UPLOAD_FAILED(1902, "Failed to upload file", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_DELETE_FAILED(1903, "Failed to delete file", HttpStatus.INTERNAL_SERVER_ERROR),

    // ===== medical result =====
    MEDICAL_RESULT_IS_COMPLETED(2001, "Medical result is already completed", HttpStatus.BAD_REQUEST),
    MEDICAL_RESULT_NOT_FOUND(2002, "Medical result not found", HttpStatus.NOT_FOUND),

    // ===== file =====
    UPLOAD_TO_VPS_FAILED(2101, "Failed to upload to server", HttpStatus.INTERNAL_SERVER_ERROR),
    TEMPLATE_FILE_NOT_FOUND(2102, "Template file not found", HttpStatus.NOT_FOUND),
    CANNOT_DELETE_DEFAULT_TEMPLATE(2103, "Cannot delete default template", HttpStatus.BAD_REQUEST),
    CANNOT_REMOVE_LAST_DEFAULT_TEMPLATE(2104, "Cannot remove last default template", HttpStatus.BAD_REQUEST),
    MEDICAL_RECORD_PDF_FAILED(2105, "Failed to create medical record PDF", HttpStatus.INTERNAL_SERVER_ERROR),
    ALREADY_HAS_DEFAULT_TEMPLATE(2106, "Already has default template", HttpStatus.BAD_REQUEST),
    CANNOT_DELETE_LAST_TEMPLATE(2107, "Cannot delete last template", HttpStatus.BAD_REQUEST),
    DEFAULT_TEMPLATE_NOT_FOUND(2108, "Default template not found", HttpStatus.NOT_FOUND),
    // ====== work schedule ======
    STAFF_ID_REQUIRED(2201, "Staff ID is required", HttpStatus.BAD_REQUEST),
    SHIFT_REQUIRED(2202, "Shift is required", HttpStatus.BAD_REQUEST),
    DAYS_OF_WEEK_REQUIRED(2203, "Days of week is required", HttpStatus.BAD_REQUEST),
    START_DATE_REQUIRED(2204, "Start date is required", HttpStatus.BAD_REQUEST),
    END_DATE_REQUIRED(2205, "End date is required", HttpStatus.BAD_REQUEST),
    START_DATE_MUST_BE_NOW_OR_FUTURE(2206, "Start date must be now or future", HttpStatus.BAD_REQUEST),
    END_DATE_MUST_BE_IN_FUTURE(2207, "End date must be in future", HttpStatus.BAD_REQUEST),
    NOTE_TOO_LONG(2208, "Note is too long", HttpStatus.BAD_REQUEST),
    WORK_SCHEDULE_NOT_FOUND(2209, "Work schedule not found", HttpStatus.NOT_FOUND),
    UNAUTHORIZED_ACTION(2210, "Unauthorized action", HttpStatus.FORBIDDEN),
    WORK_SCHEDULE_ALREADY_CHECKED_IN(2211, "Work schedule already checked in", HttpStatus.BAD_REQUEST),
    CHECKIN_DATE_INVALID(2212, "Checkin date must be in the same day", HttpStatus.BAD_REQUEST),
    SHIFT_DATE_REQUIRED(2213, "Shift date is required", HttpStatus.BAD_REQUEST),
    CANNOT_UPDATE_PAST_SCHEDULE(2214, "Cannot update past schedule", HttpStatus.BAD_REQUEST),
    CANNOT_MOVE_SCHEDULE_TO_PAST(2215, "Cannot move schedule to past", HttpStatus.BAD_REQUEST),
    CANNOT_UPDATE_ATTENDED_SCHEDULE(2216, "Cannot update attended schedule", HttpStatus.BAD_REQUEST),
    CANNOT_DELETE_ATTENDED_SCHEDULE(2217, "Cannot delete attended schedule", HttpStatus.BAD_REQUEST),
    CANNOT_DELETE_PAST_SCHEDULE(2218, "Cannot delete past schedule", HttpStatus.BAD_REQUEST),
    CANNOT_DELETE_WORK_SCHEDULE(2219, "Cannot delete work schedule", HttpStatus.BAD_REQUEST),
    CANNOT_DELETE_WORK_SCHEDULE_WITH_EMAIL_TASK(2220, "Cannot delete work schedule with email task", HttpStatus.BAD_REQUEST),
    CANNOT_REQUEST_LEAVE_FOR_PAST_SCHEDULE(2221, "Cannot request leave for past schedule", HttpStatus.BAD_REQUEST),
    LEAVE_REQUEST_TOO_CLOSE(2222, "Leave request must be before 2 days of work schedule", HttpStatus.BAD_REQUEST),
    WORK_ALREADY_ATTENDED_CANNOT_REQUEST_LEAVE(2223, "Work already attended cannot request leave", HttpStatus.BAD_REQUEST),
    REASON_REQUIRED(2224, "Reason is required", HttpStatus.BAD_REQUEST),
    DETAILS_REQUIRED(2225, "Details is required", HttpStatus.BAD_REQUEST),
    WORK_SCHEDULE_NOT_FOUND_FOR_LEAVE(2226, "Work schedule not found for leave request", HttpStatus.NOT_FOUND),
    DATE_MUST_BE_TODAY_OR_FUTURE(2227, "Date must be future", HttpStatus.BAD_REQUEST),
    LEAVE_REQUEST_STATUS_REQUIRED(2228, "Leave request status is required", HttpStatus.BAD_REQUEST),
    LEAVE_REQUEST_ID_REQUIRED(2229, "Leave request ID is required", HttpStatus.BAD_REQUEST),
    LEAVE_REQUEST_NOT_FOUND(2230, "Leave request not found", HttpStatus.NOT_FOUND),
    LEAVE_REQUEST_ALREADY_PROCESSED(2231, "Leave request already processed", HttpStatus.BAD_REQUEST),
    INVALID_LEAVE_STATUS(2232, "Invalid leave status", HttpStatus.BAD_REQUEST),
    LEAVE_REQUEST_CREATE_DATE_INVALID(2233, "Leave request create date is invalid", HttpStatus.BAD_REQUEST),
    SCHEDULE_NOT_FOUND_FOR_LEAVE(2234, "Schedule not found for leave request", HttpStatus.NOT_FOUND),
    CANNOT_APPROVE_LEAVE_FOR_ATTENDED_SHIFT(2235, "Cannot approve leave for attended shift", HttpStatus.BAD_REQUEST),
    CANNOT_DELETE_PROCESSED_LEAVE_REQUEST(2236, "Cannot delete processed leave request", HttpStatus.BAD_REQUEST),
    CANNOT_UPDATE_PROCESSED_LEAVE_REQUEST(2237, "Cannot update processed leave request", HttpStatus.BAD_REQUEST),
    WORK_SCHEDULE_ALREADY_EXISTS(2238, "Work schedule already exists", HttpStatus.BAD_REQUEST),

    SETTING_NOT_FOUND(2301, "Setting not found", HttpStatus.NOT_FOUND),

    // ===== SERVICE PACKAGE =====
    SERVICE_PACKAGE_NOT_FOUND(2301, "Service package not found", HttpStatus.NOT_FOUND),
    SERVICE_PACKAGE_NAME_REQUIRED(2302, "Package name is required", HttpStatus.BAD_REQUEST),
    SERVICE_PACKAGE_NAME_LENGTH(2203, "Package name must be between 3 and 100 characters", HttpStatus.BAD_REQUEST),
    SERVICE_PACKAGE_DESCRIPTION_LENGTH(2304, "Description must be max 500 characters", HttpStatus.BAD_REQUEST),
    DUPLICATE_SERVICE_PACKAGE_NAME(2305, "Service package with the same name already exists for this tenant", HttpStatus.CONFLICT),
    SERVICE_PACKAGE_BILLING_TYPE_REQUIRED(2306, "Billing type is required", HttpStatus.BAD_REQUEST),
    SERVICE_PACKAGE_STATUS_REQUIRED(2307, "Status is required", HttpStatus.BAD_REQUEST),
    SERVICE_PACKAGE_PRICE_REQUIRED(2308, "Price is required", HttpStatus.BAD_REQUEST),
    SERVICE_PACKAGE_PRICE_INVALID(2309, "Price must be greater than or equal to 0", HttpStatus.BAD_REQUEST),
    SERVICE_PACKAGE_DATE_INVALID(2310, "Start date must be before end date", HttpStatus.BAD_REQUEST),
    SERVICE_PACKAGE_DUPLICATE_NAME(2211, "Service package with the same name already exists for this tenant", HttpStatus.CONFLICT),
    SERVICE_PACKAGE_START_DATE_REQUIRED(2212, "Start date is required", HttpStatus.BAD_REQUEST),
    SERVICE_PACKAGE_END_DATE_INVALID(2213, "End date must not be in the past", HttpStatus.BAD_REQUEST),

    // setting
    HOSPITAL_NAME_REQUIRED(2401, "Hospital name is required", HttpStatus.BAD_REQUEST),
    HOSPITAL_NAME_LENGTH(2402, "Hospital name must be between 3 and 100 characters", HttpStatus.BAD_REQUEST),
    HOSPITAL_PHONE_REQUIRED(2403, "Hospital phone is required", HttpStatus.BAD_REQUEST),
    HOSPITAL_PHONE_LENGTH(2404, "Hospital phone must be between 3 and 100 characters", HttpStatus.BAD_REQUEST),
    HOSPITAL_EMAIL_REQUIRED(2405, "Hospital email is required", HttpStatus.BAD_REQUEST),
    HOSPITAL_EMAIL_INVALID(2406, "Hospital email is invalid", HttpStatus.BAD_REQUEST),
    HOSPITAL_ADDRESS_REQUIRED(2407, "Hospital address is required", HttpStatus.BAD_REQUEST),
    HOSPITAL_ADDRESS_LENGTH(2408, "Hospital address must be between 3 and 100 characters", HttpStatus.BAD_REQUEST),
    BANK_ACCOUNT_NUMBER_REQUIRED(2409, "Bank account number is required", HttpStatus.BAD_REQUEST),
    BANK_ACCOUNT_NUMBER_LENGTH(2410, "Bank account number must be between 3 and 100 characters", HttpStatus.BAD_REQUEST),
    BANK_CODE_REQUIRED(2411, "Bank code is required", HttpStatus.BAD_REQUEST),
    PAGING_SIZE_REQUIRED(2412, "Paging size is required", HttpStatus.BAD_REQUEST),

    // shift
    SHIFT_NAME_REQUIRED(2501, "Shift name is required", HttpStatus.BAD_REQUEST),
    SHIFT_START_TIME_REQUIRED(2502, "Shift start time is required", HttpStatus.BAD_REQUEST),
    SHIFT_END_TIME_REQUIRED(2503, "Shift end time is required", HttpStatus.BAD_REQUEST),
    SHIFT_NAME_EXISTS(2504, "Shift name already exists", HttpStatus.BAD_REQUEST),
    OVERLAPPING_TIME(2505, "Overlapping time", HttpStatus.BAD_REQUEST),
    SHIFT_NOT_FOUND(2506, "Shift not found", HttpStatus.NOT_FOUND),
    LATEST_CHECK_IN_MINUTES_REQUIRED(2507, "Latest check in minutes is required", HttpStatus.BAD_REQUEST),
    LATEST_CHECK_IN_MINUTES_MAX_60(2509, "Latest check in minutes must under 60 minutes", HttpStatus.BAD_REQUEST),

    // SPECIALIZATION
    SPECIALIZATION_NAME_REQUIRED(2601, "Specialization name is required", HttpStatus.BAD_REQUEST),
    SPECIALIZATION_NAME_EXISTS(2602, "Specialization name already exists", HttpStatus.BAD_REQUEST),
    SPECIALIZATION_NOT_FOUND(2603, "Specialization not found", HttpStatus.NOT_FOUND),
    ;

    private int code;
    private String message;
    private HttpStatusCode statusCode;
}
