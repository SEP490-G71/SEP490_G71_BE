package vn.edu.fpt.medicaldiagnosis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Account;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.repository.AccountRepository;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PermissionFilter extends OncePerRequestFilter {

    private final AccountRepository accountRepository;
    private final ObjectMapper objectMapper;

    public PermissionFilter(AccountRepository accountRepository, ObjectMapper objectMapper) {
        this.accountRepository = accountRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            log.info("Request not authenticated, skipping permission check.");
            filterChain.doFilter(request, response);
            return;
        }

        String username = auth.getName();
        Account account = accountRepository.findByUsernameAndDeletedAtIsNull(username).orElse(null);

        if (account == null) {
            log.info("User '{}' not found in database", username);
            response.setStatus(ErrorCode.UNAUTHORIZED.getStatusCode().value());
            writeErrorResponse(response, ErrorCode.UNAUTHORIZED);
            return;
        }

        boolean isAdminOrSuperAdmin = account.getRoles().stream()
                .map(role -> role.getName().toUpperCase())
                .anyMatch(roleName -> roleName.equals("ADMIN") || roleName.equals("SUPER_ADMIN"));

        if (isAdminOrSuperAdmin) {
            log.info("User '{}' has ADMIN or SUPER_ADMIN role, bypassing permission check.", username);
            filterChain.doFilter(request, response);
            return;
        }

        String requiredPermission = resolvePermission(request.getRequestURI(), request.getMethod());

        log.info("Checking permission '{}' for user '{}'", requiredPermission, username);

//        account.getRoles().forEach(role -> log.info("User '{}' has role '{}'", username, role.getName()));

        boolean allowed = account.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .anyMatch(p -> p.getName().equalsIgnoreCase(requiredPermission));

//        account.getRoles().stream()
//                .flatMap(role -> role.getPermissions().stream())
//                .forEach(p -> log.info("User '{}' has permission '{}'", username, p.getName()));

        if (!allowed) {
            log.warn("User '{}' is missing permission '{}'", username, requiredPermission);
            response.setStatus(ErrorCode.UNAUTHORIZED.getStatusCode().value());
            writeErrorResponse(response, ErrorCode.UNAUTHORIZED);
            return;
        }

        log.info("User '{}' allowed with permission '{}'", username, requiredPermission);
        filterChain.doFilter(request, response);
    }

    private void writeErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ApiResponse<String> apiResponse = ApiResponse.<String>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        String json = objectMapper.writeValueAsString(apiResponse);
        response.getWriter().write(json);
    }

    private String resolvePermission(String path, String method) {
        String[] parts = path.split("/");

        List<String> filtered = Arrays.stream(parts)
                .filter(p -> !p.isBlank())
                .filter(p -> !p.matches("[0-9a-fA-F\\-]{36}")) // UUID
                .filter(p -> !p.matches("\\d+"))               // số
                .toList();

        String entity = filtered.stream()
                .skip(1) // bỏ "medical-diagnosis"
                .findFirst()
                .map(String::toUpperCase)
                .orElse("UNKNOWN");

        String action = switch (method.toUpperCase()) {
            case "GET" -> "READ";
            case "POST" -> "CREATE";
            case "PUT", "PATCH" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> "UNKNOWN";
        };

        return entity + ":" + action;
    }


}
