package com._6.ems.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),

    UNAUTHENTICATED(1007, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1008, "You do not have permission", HttpStatus.FORBIDDEN),
    ACCESS_DENIED(1009, "You do not have permission", HttpStatus.FORBIDDEN),

    INVALID_KEY(1001, "Invalid key", HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002, "User existed", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1003, "Username must be at least {min} characters", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1004, "Password must be at least {min} characters", HttpStatus.BAD_REQUEST),
    ACCOUNT_NOT_EXISTED(1005, "User not existed", HttpStatus.NOT_FOUND),

    PROFILE_NOT_EXISTED(1006, "Profile not existed",HttpStatus.NOT_FOUND),
    INVALID_DOB(1009, "Your age must be at least {min}", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL_FORMAT(1010, "Invalid email format", HttpStatus.BAD_REQUEST),
    INVALID_PHONE_NUMBER_LENGTH(1011, "Phone number length must be exactly 10 digits", HttpStatus.BAD_REQUEST),
    PHONE_NUMBER_MUST_BE_DIGITS(1012, "Phone number must be digits", HttpStatus.BAD_REQUEST),

    EMPLOYEE_NOT_FOUND(2001, "Employee not found", HttpStatus.NOT_FOUND),
    MANAGER_NOT_FOUND(2002, "Manager not found", HttpStatus.NOT_FOUND),

    ATTENDANCE_RECORD_NOT_FOUND(3001, "Attendance record not found", HttpStatus.NOT_FOUND),
    ATTENDANCE_ALREADY_CHECKIN(3002, "Already check-in", HttpStatus.CONFLICT),
    ATTENDANCE_ALREADY_CHECKOUT(3003, "Already check-out", HttpStatus.CONFLICT),
    ATTENDANCE_NOT_CHECKIN(3004, "Must check-in before check-out", HttpStatus.CONFLICT),
    ATTENDANCE_INVALID_TIME(3005, "Invalid time", HttpStatus.CONFLICT),

    DEPARTMENT_NOT_FOUND(4001, "Department not found", HttpStatus.NOT_FOUND),
    DEPARTMENT_ALREADY_ASSIGNED(4002, "Department already has a manager assigned", HttpStatus.CONFLICT),
    EMPLOYEE_NOT_BELONG(4003, "Employee not belong to this department", HttpStatus.NOT_FOUND),

    PROJECT_NOT_FOUND(5001, "Project not found", HttpStatus.NOT_FOUND),
    EXCEED_MAX_PARTICIPANTS(5002, "Project is full", HttpStatus.CONFLICT),

    FILE_NOT_FOUND(404, "File not found", HttpStatus.NOT_FOUND),
    TASK_NOT_FOUND(404, "Task not found", HttpStatus.NOT_FOUND),

    NOTIFICATION_NOT_FOUND(404, "Notification not found", HttpStatus.NOT_FOUND),
    NOTIFICATION_ONLY_BE_SENT_BY_MANAGER(8001, "Only manager can send notification", HttpStatus.FORBIDDEN),

    PRIVILEGE_NOT_FOUND(404, "Privilege not found", HttpStatus.NOT_FOUND),

    MEETING_ROOM_NOT_FOUND(404, "Meeting room not found", HttpStatus.NOT_FOUND),
    MEETING_ROOM_CONFLICT(405, "Room is already booked for this time period", HttpStatus.CONFLICT),

    PERSONNEL_NOT_FOUND(404, "Personnel not found", HttpStatus.NOT_FOUND),
    MEETING_BOOKING_NOT_FOUND(404, "Meeting booking not found", HttpStatus.NOT_FOUND),
    EMAIL_EXCEPTION(500, "Email exception", HttpStatus.INTERNAL_SERVER_ERROR),

    ACCOUNT_NOT_FOUND(404, "Account not found", HttpStatus.NOT_FOUND),
    NO_RECIPIENT_FOUND(404, "No recipient found", HttpStatus.NOT_FOUND),
    ONLY_ADMIN_CAN_SEND_GLOBAL_NOTIFICATION(405, "Only admin can send global notification", HttpStatus.CONFLICT),
    SALARY_NOT_FOUND(404, "Salary not found", HttpStatus.NOT_FOUND),
    ;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

}
