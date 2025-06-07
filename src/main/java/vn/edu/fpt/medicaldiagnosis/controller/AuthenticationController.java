package vn.edu.fpt.medicaldiagnosis.controller;

import java.text.ParseException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.fpt.medicaldiagnosis.dto.request.*;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.AuthenticationResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.IntrospectResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Tenant;
import vn.edu.fpt.medicaldiagnosis.service.AuthenticationService;
import vn.edu.fpt.medicaldiagnosis.service.TenantService;
import vn.edu.fpt.medicaldiagnosis.service.impl.AuthenticationServiceImpl;
import com.nimbusds.jose.JOSEException;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private TenantService tenantService;

    @PostMapping("/login")
    public ApiResponse<AuthenticationResponse> login(@RequestBody AuthenticationRequest request) {
        AuthenticationResponse authResponse = authenticationService.authenticate(request);

        return ApiResponse.<AuthenticationResponse>builder()
                .result(authResponse)
                .build();
    }

    @PostMapping("/register-tenant")
    public ApiResponse<Tenant> register(@RequestBody TenantRequest request) {
        Tenant tenant = tenantService.createTenant(request);
        return ApiResponse.<Tenant>builder()
                .result(tenant)
                .build();
    }

    @PostMapping("/introspect")
    public ApiResponse<IntrospectResponse> authenticate(@RequestBody IntrospectRequest request)
            throws ParseException, JOSEException {
        IntrospectResponse result = authenticationService.introspect(request);

        return ApiResponse.<IntrospectResponse>builder().result(result).build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody LogoutRequest request) throws ParseException, JOSEException {
        authenticationService.logout(request);

        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/refreshToken")
    public ApiResponse<AuthenticationResponse> refreshToken(@RequestBody RefreshTokenRequest request)
            throws ParseException, JOSEException {
        AuthenticationResponse result = authenticationService.refreshToken(request);

        return ApiResponse.<AuthenticationResponse>builder().result(result).build();
    }
}
