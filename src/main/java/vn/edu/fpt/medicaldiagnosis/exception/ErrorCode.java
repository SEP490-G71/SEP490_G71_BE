package vn.edu.fpt.medicaldiagnosis.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum ErrorCode {
    UNCATEGORIZED(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Invalid message key", HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002, "User already exists", HttpStatus.BAD_REQUEST),
    PASSWORD_INVAlID(1003, "Password must be at least {min} characters", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(1004, "User not found", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1005, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1006, "You don't have permission", HttpStatus.FORBIDDEN),
    INVALID_DOB(1007, "The age must be at least {min} years old", HttpStatus.BAD_REQUEST),

    // Department
    DEPARTMENT_NAME_EMPTY(400, "Department name cannot be empty", HttpStatus.BAD_REQUEST),
    DEPARTMENT_NAME_LENGTH(400, "Department name must be between 3 and 100 characters", HttpStatus.BAD_REQUEST),
    DEPARTMENT_DESCRIPTION_LENGTH(400, "Description must be between 3 and 500 characters", HttpStatus.BAD_REQUEST),
    DEPARTMENT_ROOM_EMPTY(400, "Room number cannot be empty", HttpStatus.BAD_REQUEST),
    DEPARTMENT_ROOM_LENGTH(400, "Room number must be between 2 and 5 characters", HttpStatus.BAD_REQUEST),
    DEPARTMENT_TYPE_EMPTY(400, "Department Type cannot be null", HttpStatus.BAD_REQUEST),
    DEPARTMENT_NOT_FOUND(404, "Department not found", HttpStatus.NOT_FOUND),
    DEPARTMENT_ROOM_EXISTED(409, "Room number already exists", HttpStatus.BAD_REQUEST),

    // staff
    STAFF_NAME_EMPTY(400, "Staff name cannot be empty", HttpStatus.BAD_REQUEST),
    STAFF_NAME_LENGTH(400, "Staff name must be between 3 and 100 characters", HttpStatus.BAD_REQUEST),
    STAFF_SPECIALTY_EMPTY(400, "Specialty cannot be empty", HttpStatus.BAD_REQUEST),
    STAFF_LEVEL_EMPTY(400, "Level cannot be empty", HttpStatus.BAD_REQUEST),
    STAFF_PHONE_EMPTY(400, "Phone number cannot be empty", HttpStatus.BAD_REQUEST),
    STAFF_PHONE_INVALID(400, "Phone number must be 10 digits", HttpStatus.BAD_REQUEST),
    STAFF_EMAIL_EMPTY(400, "Email cannot be empty", HttpStatus.BAD_REQUEST),
    STAFF_EMAIL_LENGTH(400, "Email must be between 3 and 100 characters", HttpStatus.BAD_REQUEST),
    STAFF_EMAIL_INVALID(400, "Invalid email format", HttpStatus.BAD_REQUEST),
    STAFF_GENDER_EMPTY(400, "Gender cannot be empty", HttpStatus.BAD_REQUEST),
    STAFF_DOB_EMPTY(400, "Date of birth cannot be empty", HttpStatus.BAD_REQUEST),
    STAFF_DOB_PAST(400, "Date of birth cannot be in the future", HttpStatus.BAD_REQUEST),
    STAFF_EMAIL_EXISTED(409, "Email already exists", HttpStatus.BAD_REQUEST),
    STAFF_PHONE_EXISTED(409, "Phone number already exists", HttpStatus.BAD_REQUEST),
    STAFF_ACCOUNT_NOT_FOUND(404, "Account not found", HttpStatus.NOT_FOUND),
    STAFF_NOT_FOUND(404, "Staff not found", HttpStatus.NOT_FOUND),
    STAFF_ACCOUNT_EXISTED(409, "Account already exists", HttpStatus.BAD_REQUEST),
    TENANT_CODE_EXISTED(1008, "Tenant code already existed", HttpStatus.BAD_REQUEST),
    ROLE_NOT_FOUND(1009, "Role not found", HttpStatus.NOT_FOUND),
    PERMISSION_NOT_FOUND(1010, "Permission not found", HttpStatus.NOT_FOUND)
    ;

    private int code;

    private String message;

    private HttpStatusCode statusCode;
}
