package com._6.ems.exception;

import com._6.ems.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.metadata.ConstraintDescriptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;
import java.util.Objects;

/**
 * Global exception handler for centralized error management across the entire application.
 * Captures and processes all exceptions thrown by controllers and services.
 * Exception handlers map internal exceptions to human-readable {@link ErrorCode} and custom HTTP status codes.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String MIN_ATTRIBUTE = "min";

    /**
     * Handles any unexpected {@link RuntimeException} or generic {@link Exception}.
     * Used as a fallback for uncategorized errors — e.g., null pointer, parsing errors, etc.
     *
     * @param exception the thrown runtime exception
     * @return a standardized {@link ApiResponse} with HTTP 400 and error code 9999
     */
    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse<Void>> handlingRuntimeException(RuntimeException exception){
        log.error("Unhandled exception: ", exception);
        ApiResponse<Void> apiResponse = new ApiResponse<>();

        apiResponse.setCode(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode());
        apiResponse.setMessage(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());

        // Return 400 Bad Request for uncategorized server-side errors
        return ResponseEntity.badRequest().body(apiResponse);
    }

    /**
     * Handles custom {@link AppException} thrown intentionally by the application.
     * Each {@link AppException} wraps a specific {@link ErrorCode} with HTTP status, code, and predefined message
     * @param exception the custom {@link AppException}
     * @return a structured {@link ApiResponse} with the mapped error code and status
     */
    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse<Void>> handlingAppException(AppException exception){
        ErrorCode errorCode = exception.getErrorCode();

        ApiResponse<Void> apiResponse = new ApiResponse<>();
        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());

        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(apiResponse);
    }

    /**
     * Handles {@link AccessDeniedException} when the authenticated user lacks required permissions.
     * This is triggered by Spring Security when access control annotations (e.g., @PreAuthorize)
     *
     * @param exception the thrown {@link AccessDeniedException}
     * @return 403 Forbidden with standardized error payload
     */
    @ExceptionHandler(value = AccessDeniedException.class)
    ResponseEntity<ApiResponse<Void>> handlingAccessDeniedException(AccessDeniedException exception){
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;

        return ResponseEntity.status(errorCode.getStatusCode())
                .body(ApiResponse.<Void>builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    /**
     * Handles {@link MethodArgumentNotValidException} thrown when a request fails Bean Validation checks
     * (e.g., @NotBlank, @Min, custom annotations).
     * Extracts validation messages or codes (if mapped to {@link ErrorCode}) and replaces placeholders
     * (e.g., "{min}") with actual constraint attributes.
     *
     * @param exception validation exception containing constraint violations
     * @return a structured {@link ApiResponse} describing validation errors
     */
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> handlingValidation(MethodArgumentNotValidException exception){
        String enumKey = exception.getFieldError().getDefaultMessage();
        ErrorCode errorCode = ErrorCode.INVALID_KEY;
        Map<String, Object> attributes = null;
        try {
            // Try to map validation message (enumKey) to an existing ErrorCode enum
            errorCode = ErrorCode.valueOf(enumKey);

            // Extract constraint violation details (e.g., min/max values)
            ConstraintViolation<?> constraintViolation = exception.getBindingResult()
                    .getAllErrors().getFirst()
                    .unwrap(ConstraintViolation.class);

            ConstraintDescriptor<?> descriptor = constraintViolation.getConstraintDescriptor();

            attributes = descriptor.getAttributes();

            log.info("Validation attributes: {}", attributes);

        } catch (IllegalArgumentException e){
            log.info("Unrecognized validation key: {}", e.getMessage());
        }

        ApiResponse<Void> apiResponse = new ApiResponse<>();
        apiResponse.setCode(errorCode.getCode());

        // Replace placeholders (e.g., "{min}") in message with actual constraint values
        apiResponse.setMessage(Objects.nonNull(attributes)
                ? mapAttribute(errorCode.getMessage(), attributes)
                : errorCode.getMessage());

        return ResponseEntity.badRequest().body(apiResponse);
    }


    /**
     * Handles file upload size violations.
     * Triggered when the uploaded file exceeds configured maximum upload size.
     *
     * @param exception exception indicating file too large
     * @return HTTP 413 (Payload Too Large) with user-friendly message
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> handleMaxSizeException(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body("File size exceeds the maximum allowed limit!");
    }

    /* Helper methods */
    /**
     * Replaces placeholders (like "{min}") in validation messages with real constraint attributes.
     * Example:
     * message = "Field must be at least {min} characters"
     * attributes = {"min" -> 3}
     * result = "Field must be at least 3 characters"
     *
     * @param message    original message containing placeholders
     * @param attributes constraint attributes (e.g., {min=3, max=255})
     * @return message with replaced attributes
     */
    private String mapAttribute(String message, Map<String, Object> attributes){
        String minValue = String.valueOf(attributes.get(MIN_ATTRIBUTE));

        return message.replace("{" + MIN_ATTRIBUTE + "}", minValue);
    }
}
