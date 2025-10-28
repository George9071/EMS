package com._6.ems.configuration;

import com._6.ems.dto.request.IntrospectRequest;
import com._6.ems.service.AuthenticationService;
import com.nimbusds.jose.JOSEException;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.text.ParseException;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomJwtDecoder implements JwtDecoder {

    @Value("${jwt.signerKey}")
    private String signerKey;

    private final AuthenticationService authenticationService;

    private NimbusJwtDecoder nimbusJwtDecoder = null;

    /**
     * Decodes and validates a JWT token.
     * @param token the JWT string to decode
     * @return a {@link Jwt} object containing claims if valid
     * @throws JwtException if the token is invalid or signature verification fails
     */
    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            // Perform external introspection
            var response = authenticationService.introspect(IntrospectRequest.builder()
                    .token(token)
                    .build());

            // If introspection fails, reject the token
            if (!response.isValid()) throw new JwtException("Token invalid");

        } catch (JOSEException | ParseException e) {
            throw new JwtException(e.getMessage());
        }

        // Lazily initialize the NimbusJwtDecoder if not already created
        if (Objects.isNull(nimbusJwtDecoder)) {
            // Create an HMAC-SHA512 secret key for verifying the JWT signature
            SecretKeySpec secretKeySpec = new SecretKeySpec(signerKey.getBytes(), "HS512");

            nimbusJwtDecoder = NimbusJwtDecoder
                    .withSecretKey(secretKeySpec)
                    .macAlgorithm(MacAlgorithm.HS512)
                    .build();
        }

        // Decode and verify the JWT token
        return nimbusJwtDecoder.decode(token);
    }
}
