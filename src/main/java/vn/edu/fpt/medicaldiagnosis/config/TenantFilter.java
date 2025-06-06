package vn.edu.fpt.medicaldiagnosis.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.edu.fpt.medicaldiagnosis.context.TenantContext;

import java.io.IOException;

@Component
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 1. Ưu tiên lấy từ Header nếu có
        String tenantId = request.getHeader("X-Tenant-ID");
        System.out.println("tenantId: " + tenantId);

        System.out.println("Subdomain: " + request.getServerName());

        // 2. Nếu không có thì fallback lấy từ subdomain
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = extractTenantFromSubdomain(request.getServerName());
        }

        if (tenantId != null && !tenantId.isBlank()) {
            TenantContext.setTenantId(tenantId);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String extractTenantFromSubdomain(String serverName) {
        // Ex: tenant1.example.com → ["tenant1", "example", "com"]
        String[] parts = serverName.split("\\.");
        if (parts.length >= 3) {
            return parts[0]; // Lấy subdomain đầu tiên làm tenantId
        }
        return null;
    }
}

