package vn.springboot.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Central catalog of business error codes.
 * Each entry maps a stable numeric {@code code} (returned in the JSON body)
 * to a default {@code message} and the {@link HttpStatus} to respond with.
 */
@Getter
public enum ErrorCode {

    // 9xxx - generic / server
    EMAIL_SEND_FAILED(9000, "Failed to send email", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_STORAGE_FAILED(9001, "Failed to store file", HttpStatus.INTERNAL_SERVER_ERROR),
    UNCATEGORIZED(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),

    // 4000 - bad request / validation
    INVALID_REQUEST(4000, "Invalid request", HttpStatus.BAD_REQUEST),
    VALIDATION_ERROR(4001, "Validation failed", HttpStatus.BAD_REQUEST),
    FILE_EMPTY(4002, "Uploaded file is empty", HttpStatus.BAD_REQUEST),
    INVALID_FILE_TYPE(4003, "Unsupported file type", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE(4004, "Uploaded file is too large", HttpStatus.PAYLOAD_TOO_LARGE),

    // 401x - authentication
    UNAUTHENTICATED(4010, "Authentication required", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(4011, "Invalid username or password", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN(4012, "Invalid or malformed token", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(4013, "Token has expired", HttpStatus.UNAUTHORIZED),
    ACCOUNT_DISABLED(4014, "Account is disabled", HttpStatus.UNAUTHORIZED),

    // 403x - authorization
    ACCESS_DENIED(4030, "You do not have permission to access this resource", HttpStatus.FORBIDDEN),

    // 404x - not found
    USER_NOT_FOUND(4040, "User not found", HttpStatus.NOT_FOUND),
    ROLE_NOT_FOUND(4041, "Role not found", HttpStatus.NOT_FOUND),
    RESOURCE_NOT_FOUND(4042, "Resource not found", HttpStatus.NOT_FOUND),
    REFRESH_TOKEN_NOT_FOUND(4043, "Refresh token not found", HttpStatus.NOT_FOUND),
    NOTIFICATION_NOT_FOUND(4044, "Notification not found", HttpStatus.NOT_FOUND),

    // 409x - conflict
    USERNAME_EXISTED(4090, "Username already exists", HttpStatus.CONFLICT),
    EMAIL_EXISTED(4091, "Email already exists", HttpStatus.CONFLICT),
    REFRESH_TOKEN_REVOKED(4092, "Refresh token has been revoked", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_EXPIRED(4093, "Refresh token has expired", HttpStatus.UNAUTHORIZED);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
