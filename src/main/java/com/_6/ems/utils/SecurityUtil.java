package com._6.ems.utils;

import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.Authentication;

public class SecurityUtil {
    public static String getCurrentUserCode() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof Jwt jwt) {
            return jwt.getClaimAsString("code");
        }

        throw new AppException(ErrorCode.UNAUTHORIZED);
    }
}
