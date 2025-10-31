package com._6.ems.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {

    // =====================================================
    // 0xxx - General & System
    // =====================================================
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    EMAIL_EXCEPTION(100, "Email sending error", HttpStatus.INTERNAL_SERVER_ERROR),

    // =====================================================
    // 1xxx - Authentication & Authorization
    // =====================================================
    UNAUTHENTICATED(1001, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1002, "You do not have permission", HttpStatus.FORBIDDEN),
    ACCESS_DENIED(1003, "Access denied", HttpStatus.FORBIDDEN),
    INVALID_KEY(1004, "Nhập input sai", HttpStatus.BAD_REQUEST),

    // =====================================================
    // 11xx - Account & User
    // =====================================================
    USER_EXISTED(1101, "User already exists", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1102, "Username must be at least {min} characters", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1103, "Password must be at least {min} characters", HttpStatus.BAD_REQUEST),
    ACCOUNT_NOT_FOUND(1104, "Account not found", HttpStatus.NOT_FOUND),
    ACCOUNT_NOT_EXISTED(1105, "User does not exist", HttpStatus.NOT_FOUND),
    PROFILE_NOT_EXISTED(1106, "Profile not found", HttpStatus.NOT_FOUND),
    INVALID_DOB(1107, "Age must be at least {min}", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL_FORMAT(1108, "Invalid email format", HttpStatus.BAD_REQUEST),
    INVALID_PHONE_NUMBER_LENGTH(1109, "Phone number must be exactly 10 digits", HttpStatus.BAD_REQUEST),
    PHONE_NUMBER_MUST_BE_DIGITS(1110, "Phone number must contain only digits", HttpStatus.BAD_REQUEST),

    // =====================================================
    // 2xxx - Personnel / Employee / Manager
    // =====================================================
    PERSONNEL_NOT_FOUND(2001, "Personnel not found", HttpStatus.NOT_FOUND),
    EMPLOYEE_NOT_FOUND(2002, "Employee not found", HttpStatus.NOT_FOUND),
    MANAGER_NOT_FOUND(2003, "Manager not found", HttpStatus.NOT_FOUND),

    // =====================================================
    // 3xxx - Attendance
    // =====================================================
    ATTENDANCE_RECORD_NOT_FOUND(3001, "Attendance record not found", HttpStatus.NOT_FOUND),
    ATTENDANCE_ALREADY_CHECKIN(3002, "Already checked in", HttpStatus.CONFLICT),
    ATTENDANCE_ALREADY_CHECKOUT(3003, "Already checked out", HttpStatus.CONFLICT),
    ATTENDANCE_NOT_CHECKIN(3004, "Must check in before check out", HttpStatus.CONFLICT),
    ATTENDANCE_INVALID_TIME(3005, "Invalid attendance time", HttpStatus.BAD_REQUEST),
    ATTENDANCE_NOT_YET(3006, "Haven’t checked in yet", HttpStatus.BAD_REQUEST),
    // =====================================================
    // 4xxx - Department
    // =====================================================
    DEPARTMENT_NOT_FOUND(4001, "Department not found", HttpStatus.NOT_FOUND),
    DEPARTMENT_ALREADY_ASSIGNED(4002, "Department already has a manager", HttpStatus.CONFLICT),
    EMPLOYEE_NOT_BELONG(4003, "Employee does not belong to this department", HttpStatus.BAD_REQUEST),

    // =====================================================
    // 5xxx - Project & Task
    // =====================================================
    PROJECT_NOT_FOUND(5001, "Project not found", HttpStatus.NOT_FOUND),
    EXCEED_MAX_PARTICIPANTS(5002, "Project participant limit exceeded", HttpStatus.CONFLICT),
    TASK_NOT_FOUND(5003, "Task not found", HttpStatus.NOT_FOUND),

    // =====================================================
    // 6xxx - File & Media
    // =====================================================
    FILE_NOT_FOUND(6001, "File not found", HttpStatus.NOT_FOUND),

    // =====================================================
    // 7xxx - Notification
    // =====================================================
    NOTIFICATION_NOT_FOUND(7001, "Notification not found", HttpStatus.NOT_FOUND),
    NOTIFICATION_ONLY_BE_SENT_BY_MANAGER(7002, "Only managers can send notifications", HttpStatus.FORBIDDEN),
    NO_RECIPIENT_FOUND(7003, "No recipient found", HttpStatus.NOT_FOUND),
    ONLY_ADMIN_CAN_SEND_GLOBAL_NOTIFICATION(7004, "Only admins can send global notifications", HttpStatus.FORBIDDEN),

    // =====================================================
    // 8xxx - Meeting Room
    // =====================================================
    MEETING_ROOM_NOT_FOUND(8001, "Meeting room not found", HttpStatus.NOT_FOUND),
    MEETING_ROOM_CONFLICT(8002, "Meeting room is already booked for this time", HttpStatus.CONFLICT),
    MEETING_BOOKING_NOT_FOUND(8003, "Meeting booking not found", HttpStatus.NOT_FOUND),
    UNAUTHORIZED_UPDATE_BOOKING(8020, "You are not authorized to update this booking", HttpStatus.FORBIDDEN),
    UNAUTHORIZED_DELETE_BOOKING(8021, "You are not authorized to delete this booking", HttpStatus.FORBIDDEN),
    MEETING_ROOM_NAME_EXISTED(8002, "Meeting room name already exists", HttpStatus.BAD_REQUEST),
    MEETING_ROOM_HAS_FUTURE_BOOKINGS(8003, "Cannot delete room with future bookings", HttpStatus.BAD_REQUEST),
    // =====================================================
    // 9xxx - Privilege & Role
    // =====================================================
    PRIVILEGE_NOT_FOUND(9001, "Privilege not found", HttpStatus.NOT_FOUND),
    // =====================================================
    // 10xxx - Privilege & Role
    // =====================================================
    SALARY_NOT_FOUND(10001, "Salary not found", HttpStatus.NOT_FOUND),
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
