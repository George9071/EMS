package com._6.ems.service;

import com._6.ems.dto.request.AuthenticationRequest;
import com._6.ems.dto.request.IntrospectRequest;
import com._6.ems.dto.request.LogoutRequest;
import com._6.ems.dto.request.RefreshRequest;
import com._6.ems.dto.response.AuthenticationResponse;
import com._6.ems.dto.response.IntrospectResponse;
import com._6.ems.entity.Account;
import com._6.ems.entity.InvalidatedToken;
import com._6.ems.entity.Personnel;
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

    public AuthenticationResponse authenticate(AuthenticationRequest request){
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        var user = accountRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_EXISTED));

        boolean authenticated = passwordEncoder.matches(request.getPassword(),
                user.getPassword());

        if (!authenticated)
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        var token = generateToken(user);

        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .build();
    }

    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        try {
            var signToken = verifyToken(request.getToken(), true);

            String jit = signToken.getJWTClaimsSet().getJWTID();
            Date expiryTime = signToken.getJWTClaimsSet().getExpirationTime();

            InvalidatedToken invalidatedToken =
                    InvalidatedToken.builder().id(jit).expiryTime(expiryTime).build();

            invalidatedTokenRepository.save(invalidatedToken);
        } catch (AppException exception){
            log.info("Token already expired");
        }
    }

    public AuthenticationResponse refreshToken(RefreshRequest request)
            throws ParseException, JOSEException {

        var signedJWT = verifyToken(request.getToken(), true);
        var jit = signedJWT.getJWTClaimsSet().getJWTID();
        var expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                .id(jit)
                .expiryTime(expiryTime)
                .build();

        invalidatedTokenRepository.save(invalidatedToken);

        var username = signedJWT.getJWTClaimsSet().getSubject();

        var user = accountRepository.findByUsername(username).orElseThrow(
                () -> new AppException(ErrorCode.UNAUTHENTICATED)
        );

        return AuthenticationResponse.builder()
                .token(generateToken(user))
                .authenticated(true)
                .build();
    }


    /* Helper method */
    private String generateToken(Account account) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        Personnel personnel = personnelRepository.findByAccount_Id(account.getId())
                .orElse(null);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(account.getUsername())
                .issuer("manarate.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli()
                ))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", account.getRole().name())
                .claim("privilege", buildPrivilege(account))
                .claim("code", personnel != null ? personnel.getCode() : 0)
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new RuntimeException(e);
        }
    }

    private SignedJWT verifyToken(String token, boolean isRefresh) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());

        SignedJWT signedJWT = SignedJWT.parse(token);

        Date expiryTime = (isRefresh)
                ? new Date(signedJWT.getJWTClaimsSet().getIssueTime()
                .toInstant().plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS).toEpochMilli())
                : signedJWT.getJWTClaimsSet().getExpirationTime();

        var verified = signedJWT.verify(verifier);

        if (!(verified && expiryTime.after(new Date())))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        if (invalidatedTokenRepository
                .existsById(signedJWT.getJWTClaimsSet().getJWTID()))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        return signedJWT;
    }

    private String buildPrivilege(Account account){
        Optional<Personnel> personnelOpt = personnelRepository.findByAccount_Id(account.getId());
        if (personnelOpt.isEmpty()) {
            return "";
        }
        StringJoiner stringJoiner = new StringJoiner(" ");
        Personnel personnel = personnelOpt.get();

        if (!CollectionUtils.isEmpty(personnel.getPrivileges())) {
            personnel.getPrivileges().forEach(privilege -> {

                stringJoiner.add("AUTHORIZE_" + privilege.getName());

                if (!CollectionUtils.isEmpty(privilege.getPermissions())) {
                    privilege.getPermissions()
                            .forEach(permission -> stringJoiner.add(permission.getName()));
                }
            });
        }

        return stringJoiner.toString();
    }
}
