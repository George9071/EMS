package com._6.ems.converter;

import org.springframework.lang.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;

public class CustomJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public @NonNull Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // 1. privilege claim -> raw authorities (e.g., CREATE_POST, REJECT_POST)
        Object privilegeClaim = jwt.getClaim("privilege");
        if (privilegeClaim instanceof String privilegeStr) {
            Arrays.stream(privilegeStr.split(" "))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
        }

        // 2. scope claim -> ROLE_xxx
        Object scopeClaim = jwt.getClaim("scope");
        if (scopeClaim instanceof String scopeStr) {
            Arrays.stream(scopeStr.split(" "))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(role -> "ROLE_" + role)
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
        }

        return authorities;
    }
}
