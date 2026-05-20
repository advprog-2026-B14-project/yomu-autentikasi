package id.ac.ui.cs.advprog.yomuauth.filter;

import id.ac.ui.cs.advprog.yomuauth.model.User;
import id.ac.ui.cs.advprog.yomuauth.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private AuthService authService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDoFilterInternal_NoAuthorizationHeader() throws ServletException, IOException {
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
        verify(authService, never()).verifyTokenAndGetUser(anyString());
    }

    @Test
    void testDoFilterInternal_InvalidHeaderFormat() throws ServletException, IOException {
        request.addHeader("Authorization", "InvalidFormatToken");

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
        verify(authService, never()).verifyTokenAndGetUser(anyString());
    }

    @Test
    void testDoFilterInternal_Success() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer valid-token");

        User mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail("user@example.com");
        mockUser.setRole("USER");

        when(authService.verifyTokenAndGetUser("valid-token")).thenReturn(mockUser);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        User principal = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertEquals("user@example.com", principal.getEmail());
        assertEquals("ROLE_USER", SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority());

        verify(filterChain, times(1)).doFilter(request, response);
        verify(authService, times(1)).verifyTokenAndGetUser("valid-token");
    }

    @Test
    void testDoFilterInternal_InvalidTokenException() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer invalid-token");

        when(authService.verifyTokenAndGetUser("invalid-token")).thenThrow(new RuntimeException("Token tidak valid"));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
        verify(authService, times(1)).verifyTokenAndGetUser("invalid-token");
    }
}
