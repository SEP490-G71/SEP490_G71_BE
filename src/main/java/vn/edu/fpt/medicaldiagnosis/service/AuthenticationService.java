package vn.edu.fpt.medicaldiagnosis.service;

import vn.edu.fpt.medicaldiagnosis.dto.request.*;
import vn.edu.fpt.medicaldiagnosis.dto.response.AuthenticationResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.IntrospectResponse;

import com.nimbusds.jose.JOSEException;

import java.text.ParseException;

public interface AuthenticationService {

    AuthenticationResponse authenticate(AuthenticationRequest request);

    IntrospectResponse introspect(IntrospectRequest request) throws ParseException, JOSEException;

    void logout(LogoutRequest request) throws ParseException, JOSEException;

    AuthenticationResponse refreshToken(RefreshTokenRequest request) throws ParseException, JOSEException;

    void forgotPassword(ForgetPasswordRequest request);
}
