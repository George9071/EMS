package com._6.ems.converter;

import org.springframework.lang.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;

/**
 * Custom converter that extracts granted authorities (roles and privileges)
 * from JWT claims and transforms them into Spring Security {@link GrantedAuthority} objects.
 *
 * Allows Spring Security to understand authorization information
 * embedded inside the JWT — typically claims such as "scope" and "privilege"
 *
 * "privilege" - action-based authorities (e.g., "CREATE_TASK", "REJECT_TASK")
 * "scope" - role-based authorities (e.g., "EMPLOYEE", "ADMIN"), automatically prefixed with "ROLE_"
 */
public class CustomJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    /**
     * Converts JWT claims into a collection of {@link GrantedAuthority} objects.
     *
     * @param jwt the decoded {@link Jwt} token containing user claims
     * @return a collection of {@link GrantedAuthority} extracted from the token
     */
    @Override
    public @NonNull Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // 1. "privilege" claim -> action authorities
        Object privilegeClaim = jwt.getClaim("privilege");
        if (privilegeClaim instanceof String privilege) {
            Arrays.stream(privilege.split(" "))       // Split by spaces
                    .map(String::trim)                      // Remove extra whitespace
                    .filter(s -> !s.isEmpty())              // Ignore empty strings
                    .map(SimpleGrantedAuthority::new)       // Convert to GrantedAuthority
                    .forEach(authorities::add);             // Add to the authority set
        }

        // 2. "scope" claim → role-based authorities ("ADMIN EMPLOYEE" → [ROLE_ADMIN, ROLE_EMPLOYEE])
        Object scopeClaim = jwt.getClaim("scope");
        if (scopeClaim instanceof String scope) {
            Arrays.stream(scope.split(" "))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(role -> "ROLE_" + role)
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
        }

        return authorities;
    }
}
