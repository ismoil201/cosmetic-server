package com.example.backend.security;

import com.example.backend.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // ✅ IMPORTANT:
        // Old code skipped public endpoints entirely, so optional-auth on public endpoints broke.
        // New behavior: ALWAYS attempt auth if Bearer token exists, even on permitAll routes.

        String authHeader = request.getHeader("Authorization");

        // No token -> proceed as anonymous (works for both public and protected; protected will be blocked by SecurityConfig)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7).trim();

        // token blank/null -> proceed
        if (token.isBlank() || token.equalsIgnoreCase("null")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String email = jwtService.extractEmail(token);
            String role = jwtService.extractRole(token);

            if (email != null && !email.isBlank()) {
                if (role == null || role.isBlank()) role = "USER";
                role = role.trim().toUpperCase();
                if (role.startsWith("ROLE_")) role = role.substring(5);

                Authentication existing = SecurityContextHolder.getContext().getAuthentication();
                boolean missing = existing == null;
                boolean anonymous = existing instanceof AnonymousAuthenticationToken;

                if (missing || anonymous) {
                    UsernamePasswordAuthenticationToken authentication =
                            UsernamePasswordAuthenticationToken.authenticated(
                                    email,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
                            );

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }

        } catch (Exception e) {
            // Invalid/expired token -> clear context and continue.
            // Protected endpoints will still fail later due to SecurityConfig.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}