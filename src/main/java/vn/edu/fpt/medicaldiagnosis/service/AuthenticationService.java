package vn.edu.fpt.medicaldiagnosis.service;

import vn.edu.fpt.medicaldiagnosis.dto.request.AuthenticationRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.IntrospectRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.LogoutRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.RefreshTokenRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.AuthenticationResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.IntrospectResponse;

import com.nimbusds.jose.JOSEException;

import java.text.ParseException;

public interface AuthenticationService {

    AuthenticationResponse authenticate(AuthenticationRequest request);

    IntrospectResponse introspect(IntrospectRequest request) throws ParseException, JOSEException;

    void logout(LogoutRequest request) throws ParseException, JOSEException;

    AuthenticationResponse refreshToken(RefreshTokenRequest request) throws ParseException, JOSEException;
}
