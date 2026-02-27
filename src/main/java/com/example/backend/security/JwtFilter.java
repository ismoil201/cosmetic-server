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

import static io.jsonwebtoken.Jwts.claims;

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

        String path = request.getRequestURI();

        if (
                path.equals("/api/products") ||
                        path.startsWith("/api/products/") ||
                        path.equals("/api/products/today-deals") ||
                        path.startsWith("/api/reviews/product/") ||
                        path.startsWith("/api/auth") ||
                        path.startsWith("/api/home")  ||
                        path.startsWith("/v3/api-docs") ||           // ✅ swagger
                        path.startsWith("/swagger-ui") ||            // ✅ swagger
                        path.equals("/swagger-ui.html") ||           // ✅ swagger shortcut
                        path.equals("/error")// ✅ ADD

        ) {
            filterChain.doFilter(request, response);
            return;
        }


        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (token.isBlank() || token.equalsIgnoreCase("null")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String email = jwtService.extractEmail(token);
            String role = jwtService.extractRole(token);
            if (role == null || role.isBlank()) role = "USER";
            role = role.toUpperCase();
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

        } catch (Exception e) {
            SecurityContextHolder.clearContext(); // 🔥 MUHIM
        }

        filterChain.doFilter(request, response);
    }



}