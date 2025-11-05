package com._6.ems.service;

import com._6.ems.dto.request.AuthenticationRequest;
import com._6.ems.dto.request.IntrospectRequest;
import com._6.ems.dto.request.LogoutRequest;
import com._6.ems.dto.request.RefreshRequest;
import com._6.ems.dto.response.AuthenticationResponse;
import com._6.ems.dto.response.IntrospectResponse;
import com._6.ems.entity.*;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.repository.AccountRepository;
import com._6.ems.repository.InvalidatedTokenRepository;
import com._6.ems.repository.PersonnelRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
    AccountRepository accountRepository;
    PersonnelRepository personnelRepository;
    InvalidatedTokenRepository invalidatedTokenRepository;

    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;

    @NonFinal
    @Value("${jwt.valid-duration}")
    protected long VALID_DURATION;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    protected long REFRESHABLE_DURATION;

    /**
     * Verifies whether a given token is valid.
     *
     * @param request contains the token to introspect
     * @return {@link IntrospectResponse} indicating whether the token is valid
     */
    public IntrospectResponse introspect(IntrospectRequest request)
            throws JOSEException, ParseException {

        var token = request.getToken();
        boolean isValid = true;

        try {
            verifyToken(token, false);
        } catch (AppException e) {
            isValid = false;
        }

        return IntrospectResponse.builder()
                .valid(isValid)
                .build();
    }

    /**
     * Authenticates the user by username and password, then issues a JWT.
     *
     * @param request contains username and password credentials
     * @return {@link AuthenticationResponse} containing a signed JWT if successful
     * @throws AppException if credentials are invalid or user not found
     */
    public AuthenticationResponse authenticate(AuthenticationRequest request){
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        var user = accountRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_EXISTED));

        boolean authenticated = passwordEncoder.matches(request.getPassword(),user.getPassword());
        if (!authenticated)
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        var token = generateToken(user);

        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .build();
    }

    /**
     * Invalidates a token by storing its ID (JTI) into the invalidation repository.
     * @param request contains the token to invalidate
     */
    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        try {
            var signToken = verifyToken(request.getToken(), true);

            String jti = signToken.getJWTClaimsSet().getJWTID();
            Date expiryTime = signToken.getJWTClaimsSet().getExpirationTime();

            InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                    .id(jti)
                    .expiryTime(expiryTime)
                    .build();

            invalidatedTokenRepository.save(invalidatedToken);
        } catch (AppException exception){
            log.info("Token already expired");
        }
    }

    /**
     * Refreshes a JWT token if it is still within the refreshable duration.
     * @param request contains the token to refresh
     * @return {@link AuthenticationResponse} containing a new valid token
     */
    public AuthenticationResponse refreshToken(RefreshRequest request)
            throws ParseException, JOSEException {

        var signedJWT = verifyToken(request.getToken(), true);

        // Invalidate old token
        var jti = signedJWT.getJWTClaimsSet().getJWTID();
        var expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        invalidatedTokenRepository.save(
                InvalidatedToken.builder().id(jti).expiryTime(expiryTime).build()
        );

        // Reissue new token
        var accountId = signedJWT.getJWTClaimsSet().getSubject();
        var user = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        return AuthenticationResponse.builder()
                .token(generateToken(user))
                .authenticated(true)
                .build();
    }


    /* Helper method */
    private String generateToken(Account account) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(VALID_DURATION);
        String jti = UUID.randomUUID().toString();

        JWTClaimsSet.Builder jwtClaimsSet = new JWTClaimsSet.Builder()
                .issuer("manarate.com")
                .subject(account.getId())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiresAt))
                .jwtID(jti);

        // ---- Custom auth claims
        jwtClaimsSet.claim("username", account.getUsername());
        jwtClaimsSet.claim("scope",  account.getRole().name());
        jwtClaimsSet.claim("privileges", buildPrivilege(account));

        // ---- Optional personnel claims
        personnelRepository.findByAccount_Id(account.getId())
                .ifPresent(personnel -> jwtClaimsSet.claim("code", personnel.getCode()));

        // Create the signed JWT (header + payload)
        JWSObject jwsObject = new JWSObject(header, new Payload(jwtClaimsSet.build().toJSONObject()));

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Verifies a JWT’s signature, expiration, and invalidation state.
     *
     * @param token     the JWT string to verify
     * @param isRefresh whether this is for token refresh (uses refreshable duration)
     * @return parsed and verified {@link SignedJWT}
     * @throws AppException if token is invalid, expired, or revoked
     */
    private SignedJWT verifyToken(String token, boolean isRefresh)
            throws JOSEException, ParseException {

        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);

        // Determine correct expiration time depending on context
        Date expiryTime = (isRefresh)
                ? new Date(signedJWT.getJWTClaimsSet().getIssueTime()
                .toInstant().plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS).toEpochMilli())
                : signedJWT.getJWTClaimsSet().getExpirationTime();

        boolean verified = signedJWT.verify(verifier);

        // Reject if signature invalid or token expired
        if (!(verified && expiryTime.after(new Date())))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        // Reject if the token was previously invalidated (e.g., logged out)
        if (invalidatedTokenRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID()))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        return signedJWT;
    }

    /**
     * Builds a concatenated string of all privileges and permissions assigned to a personnel.
     * @param account the account whose privileges are being resolved
     * @return a space-separated string of authorities
     */
    private String buildPrivilege(Account account){
        if (account == null || account.getId() == null) return "";

        return personnelRepository.findByAccount_Id(account.getId())
                .map(personnel -> {
                    StringJoiner stringJoiner = new StringJoiner(" ");
                    if (CollectionUtils.isEmpty(personnel.getPrivileges())) return "";

                    personnel.getPrivileges().forEach(privilege -> {
                        // 1. Add privilege name (e.g., "AUTHORIZE_ADMIN")
                        stringJoiner.add("AUTHORIZE_" + privilege.getName());

                        // 2. Add all associated permissions (e.g., "VIEW", "EDIT")
                        if (!CollectionUtils.isEmpty(privilege.getPermissions())) {
                            privilege.getPermissions().stream()
                                    .map(PrivilegePermission::getPermission)
                                    .map(Permission::getName)
                                    .forEach(stringJoiner::add);
                        }
                    });

                    return stringJoiner.toString();
                })
                .orElse("");
    }
}
