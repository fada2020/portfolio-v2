package com.smartwork.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Enumeration of application error codes with HTTP status mapping.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common Errors (1xxx)
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "Invalid input value"),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "C002", "Entity not found"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "Internal server error"),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C004", "Invalid type value"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C005", "Method not allowed"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "C006", "Access denied"),
    HANDLE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "C007", "Access is denied"),

    // Authentication & Authorization (2xxx)
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "Authentication required"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A002", "Invalid credentials"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "A003", "Token has expired"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A004", "Invalid token"),
    INSUFFICIENT_PERMISSION(HttpStatus.FORBIDDEN, "A005", "Insufficient permission"),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "A006", "Account is locked"),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "A007", "Account is disabled"),

    // User Management (3xxx)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "User not found"),
    DUPLICATE_USER(HttpStatus.CONFLICT, "U002", "User already exists"),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "U003", "Username already exists"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U004", "Email already exists"),
    DUPLICATE_EMPLOYEE_ID(HttpStatus.CONFLICT, "U005", "Employee ID already exists"),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "U006", "Invalid password format"),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "U007", "Password confirmation mismatch"),
    CURRENT_PASSWORD_INCORRECT(HttpStatus.BAD_REQUEST, "U008", "Current password is incorrect"),

    // Board Management (4xxx)
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "B001", "Board not found"),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "B002", "Post not found"),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "B003", "Comment not found"),
    UNAUTHORIZED_POST_ACCESS(HttpStatus.FORBIDDEN, "B004", "Unauthorized access to post"),

    // Approval Management (5xxx)
    APPROVAL_NOT_FOUND(HttpStatus.NOT_FOUND, "AP001", "Approval not found"),
    INVALID_APPROVAL_STATUS(HttpStatus.BAD_REQUEST, "AP002", "Invalid approval status"),
    APPROVAL_ALREADY_PROCESSED(HttpStatus.CONFLICT, "AP003", "Approval already processed"),
    UNAUTHORIZED_APPROVER(HttpStatus.FORBIDDEN, "AP004", "Unauthorized approver"),

    // Attendance Management (6xxx)
    ATTENDANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "AT001", "Attendance record not found"),
    DUPLICATE_ATTENDANCE(HttpStatus.CONFLICT, "AT002", "Attendance already recorded"),
    INVALID_ATTENDANCE_TIME(HttpStatus.BAD_REQUEST, "AT003", "Invalid attendance time"),

    // File Management (7xxx)
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "F001", "File not found"),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "F002", "File upload failed"),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "F003", "File size exceeded"),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "F004", "Invalid file type"),
    FILE_DOWNLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "F005", "File download failed");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
