package com.ubs.billing.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ubs.billing.exception.PasswordChangeRequiredException;
import com.ubs.billing.util.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    private static final List<String> ALLOWED_PATHS = List.of(
            "/auth/change-temporary-password",
            "/auth/logout",
            "/users/me",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    );

    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (requiresPasswordChange(request) && !isAllowedPath(request)) {
            writeErrorResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean requiresPasswordChange(HttpServletRequest request) {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            return false;
        }

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof CustomUserDetails userDetails)) {
            return false;
        }

        return Boolean.TRUE.equals(userDetails.getUser().getFirstLogin());
    }

    private boolean isAllowedPath(HttpServletRequest request) {
        String path = request.getRequestURI().replaceFirst("^/api", "");
        return ALLOWED_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private void writeErrorResponse(HttpServletResponse response) throws IOException {
        PasswordChangeRequiredException exception = new PasswordChangeRequiredException();
        ApiResponse<Void> body = ApiResponse.error(exception.getMessage(), exception.getErrorCode().getCode());
        response.setStatus(exception.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
