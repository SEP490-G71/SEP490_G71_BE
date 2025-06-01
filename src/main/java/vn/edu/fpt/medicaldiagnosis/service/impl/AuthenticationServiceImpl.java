package vn.edu.fpt.medicaldiagnosis.service.impl;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import vn.edu.fpt.medicaldiagnosis.dto.request.AuthenticationRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.IntrospectRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.LogoutRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.RefreshTokenRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.AuthenticationResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.IntrospectResponse;
import vn.edu.fpt.medicaldiagnosis.entity.InvalidatedToken;
import vn.edu.fpt.medicaldiagnosis.entity.Account;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.repository.InvalidatedTokenRepository;
import vn.edu.fpt.medicaldiagnosis.repository.AccountRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.extern.slf4j.Slf4j;
import vn.edu.fpt.medicaldiagnosis.service.AuthenticationService;

@Service
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private InvalidatedTokenRepository invalidatedTokenRepository;

    @Value("${jwt.signedKey}")
    private String SECRET_KEY;

    @Value("${jwt.valid-duration}")
    private Long VALID_DURATION;

    @Value("${jwt.refreshable-duration}")
    private Long REFRESHABLE_DURATION;

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        log.info("singed key: {}", SECRET_KEY);
        Account account = accountRepository
                .findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        log.info("account: {}", account.getPassword());

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        boolean isAuthenticated = passwordEncoder.matches(request.getPassword(), account.getPassword());        if (!isAuthenticated) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String token = generateToken(account);

        return AuthenticationResponse.builder().authenticated(true).token(token).build();
    }

    /**
     * Generate token by using MACSigner algorithm
     * Token contains 3 parts: header, payload, signature
     * @param account
     * @return a token
     */
    private String generateToken(Account account) {
        //        header: contain information about algorithm and type
        JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS512);
        String scope = buildScope(account);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(account.getUsername())
                .issuer("vn.edu.fpt.medicaldiagnosis")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", scope)
                .build();
        //        payload: contain information about subject, issuer, issue time, expiration time, customClaim
        Payload payload = new Payload(claimsSet.toJSONObject());

        //        header, payload
        JWSObject jwsObject = new JWSObject(jwsHeader, payload);

        //        Use MACSigner algorithm to sign the jws
        //        MACSigner: private key and public key are the same
        try {
            jwsObject.sign(new MACSigner(SECRET_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Error while signing token", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Verify token using algorithm MACSigner
     * @param request contains information: token
     * @return an object instance of IntrospectResponse class
     * @throws ParseException
     * @throws JOSEException
     */
    public IntrospectResponse introspect(IntrospectRequest request) throws ParseException, JOSEException {
        String token = request.getToken();
        boolean isValid = true;
        try {
            verifyToken(token, false);
        } catch (Exception ex) {
            isValid = false;
        }

        return IntrospectResponse.builder().valid(isValid).build();
    }

    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        try {
            SignedJWT signedJWT = verifyToken(request.getToken(), true);
            String jwtId = signedJWT.getJWTClaimsSet().getJWTID();
            Date expireTime = signedJWT.getJWTClaimsSet().getExpirationTime();

            InvalidatedToken invalidatedToken =
                    InvalidatedToken.builder().id(jwtId).expireTime(expireTime).build();
            invalidatedTokenRepository.save(invalidatedToken);
        } catch (AppException ex) {
            log.info("Token had already expired");
        }
    }

    public AuthenticationResponse refreshToken(RefreshTokenRequest request) throws ParseException, JOSEException {
        SignedJWT signedJWT = verifyToken(request.getToken(), true);
        String jwtId = signedJWT.getJWTClaimsSet().getJWTID();
        Date expireTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        InvalidatedToken invalidatedToken =
                InvalidatedToken.builder().id(jwtId).expireTime(expireTime).build();
        invalidatedTokenRepository.save(invalidatedToken);

        String username = signedJWT.getJWTClaimsSet().getSubject();
        Account account =
                accountRepository.findByUsername(username).orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        String token = generateToken(account);
        return AuthenticationResponse.builder().token(token).authenticated(true).build();
    }

    private SignedJWT verifyToken(String token, boolean isRefresh) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(SECRET_KEY.getBytes());

        //        Parse token to claims to get header, payload, signature
        SignedJWT signedJwt = SignedJWT.parse(token);

        Date expirationTime = isRefresh
                ? new Date(signedJwt
                        .getJWTClaimsSet()
                        .getIssueTime()
                        .toInstant()
                        .plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS)
                        .toEpochMilli())
                : signedJwt.getJWTClaimsSet().getExpirationTime();

        //        Verifies the signature of the JWT using the verifier created earlier
        boolean verified = signedJwt.verify(verifier);
        if (!(verified && expirationTime.after(new Date()))) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if (invalidatedTokenRepository.existsById(signedJwt.getJWTClaimsSet().getJWTID())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        return signedJwt;
    }

    private String buildScope(Account account) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        if (!CollectionUtils.isEmpty(account.getRoles())) {
            account.getRoles().forEach(role -> {
                stringJoiner.add("ROLE_" + role.getName());
                if (!CollectionUtils.isEmpty(role.getPermissions())) {
                    role.getPermissions().forEach(permission -> stringJoiner.add(permission.getName()));
                }
            });
        }

        return stringJoiner.toString();
    }
}
