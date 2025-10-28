package com._6.ems.configuration;

import com._6.ems.dto.response.ApiResponse;
import com._6.ems.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * Custom implementation of {@link AuthenticationEntryPoint} used to handle unauthorized access attempts.
 */
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * This method is automatically called by Spring Security when an unauthenticated request
     * tries to access a secured endpoint.
     *
     * @param request        the {@link HttpServletRequest} that triggered the exception
     * @param response       the {@link HttpServletResponse} to send the error response to
     * @param authException  the exception that caused the authentication failure
     * @throws IOException   if an input or output error occurs while writing the response
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException)
            throws IOException {

        // predefined error code for unauthenticated access
        ErrorCode errorCode = ErrorCode.UNAUTHENTICATED;

        response.setStatus(errorCode.getStatusCode().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        // Serialize the ApiResponse object into JSON
        ObjectMapper objectMapper = new ObjectMapper();

        // Write the JSON response body to the HTTP response
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));

        // Ensure that the response is fully flushed to the client
        response.flushBuffer();
    }
}
