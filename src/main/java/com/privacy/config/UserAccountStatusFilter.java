package com.privacy.config;

import com.privacy.entity.User;
import com.privacy.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Validates that any user currently holding an authenticated session still exists in the
 * database and remains enabled. If the user account has been deleted or deactivated in MySQL,
 * this filter immediately clears the SecurityContext, invalidates the HTTP session, and rejects
 * the request (HTTP 401 for API calls or redirect for browser navigations).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserAccountStatusFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            String username = auth.getName();
            Optional<User> userOpt = userRepository.findByUsername(username);

            if (userOpt.isEmpty() || !userOpt.get().isEnabled()) {
                log.warn("Authenticated session revoked: user '{}' no longer exists or is disabled.", username);
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }

                String uri = request.getRequestURI();
                if (uri.startsWith("/api/")) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"message\":\"Account has been disabled or deleted\"}");
                    return;
                } else {
                    response.sendRedirect(request.getContextPath() + "/login.html?error=account_invalid");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
