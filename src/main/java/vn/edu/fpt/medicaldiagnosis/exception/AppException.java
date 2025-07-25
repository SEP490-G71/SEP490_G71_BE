package vn.edu.fpt.medicaldiagnosis.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppException extends RuntimeException {
    private ErrorCode errorCode;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public AppException(ErrorCode errorCode, String customMessage) {
        super(customMessage); // Custom message sẽ hiển thị qua getMessage()
        this.errorCode = errorCode;
    }
}
