package com.example.backend.security;

import com.example.backend.service.JwtService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtFilterTest {

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void publicEndpoint_withBearerToken_setsSecurityContext() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        when(jwtService.extractEmail("t")).thenReturn("user@test.com");
        when(jwtService.extractRole("t")).thenReturn("USER");

        JwtFilter filter = new JwtFilter(jwtService);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/products/123");
        req.addHeader("Authorization", "Bearer t");

        MockHttpServletResponse res = new MockHttpServletResponse();

        FilterChain chain = (request, response) -> { /* continue */ };

        filter.doFilter(req, res, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getName()).isEqualTo("user@test.com");
        assertThat(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER"))).isTrue();

        verify(jwtService).extractEmail("t");
        verify(jwtService).extractRole("t");
    }

    @Test
    void publicEndpoint_withoutBearerToken_doesNotSetSecurityContext() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        JwtFilter filter = new JwtFilter(jwtService);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/products/123");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, (request, response) -> {});

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService);
    }
}