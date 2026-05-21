package id.ac.ui.cs.advprog.yomuauth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.yomuauth.dto.LoginRequest;
import id.ac.ui.cs.advprog.yomuauth.dto.RegisterRequest;
import id.ac.ui.cs.advprog.yomuauth.model.User;
import id.ac.ui.cs.advprog.yomuauth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.ResourceAccessException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testRegister_Success() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFullName("Test User");
        request.setUsername("testuser");

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");

        when(authService.register(any(RegisterRequest.class))).thenReturn(user);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    void testRegister_Failure() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFullName("Test User");
        request.setUsername("testuser");

        when(authService.register(any(RegisterRequest.class))).thenThrow(new RuntimeException("Registrasi gagal"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Registrasi gagal"));

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    void testLogin_Success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("test@example.com");
        request.setPassword("password123");

        Map<String, Object> response = new HashMap<>();
        response.put("access_token", "jwt-token");

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("jwt-token"));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    void testLogin_Timeout() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("test@example.com");
        request.setPassword("password123");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new ResourceAccessException("Connection timeout"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isGatewayTimeout())
                .andExpect(content().string(containsString("Server Supabase sedang gangguan/timeout")));
    }

    @Test
    void testLogin_Unauthorized() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("test@example.com");
        request.setPassword("password123");

        when(authService.login(any(LoginRequest.class))).thenThrow(new RuntimeException("Login gagal"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Login gagal"));
    }

    @Test
    void testLogin_RateLimiting() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("test@example.com");
        request.setPassword("password123");

        Map<String, Object> response = new HashMap<>();
        response.put("access_token", "jwt-token");

        // Mock 5 successful logins
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        // Perform 5 logins from the same remote IP
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/auth/login")
                            .remoteAddress("127.0.0.1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        // The 6th login should be rate limited (429)
        mockMvc.perform(post("/auth/login")
                        .remoteAddress("127.0.0.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(429))
                .andExpect(content().string("Terlalu banyak percobaan login. Silakan coba lagi nanti."));

        // Only 5 calls should have actually hit the service
        verify(authService, times(5)).login(any(LoginRequest.class));
    }

    @Test
    void testLogout_Success_WithBearer() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer token123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout berhasil"));

        verify(authService, times(1)).logout("token123");
    }

    @Test
    void testLogout_Success_WithoutBearer() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "token123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout berhasil"));

        verify(authService, times(1)).logout("token123");
    }

    @Test
    void testLogout_MissingHeader() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Token tidak ditemukan"));

        verify(authService, never()).logout(anyString());
    }

    @Test
    void testLogout_EmptyHeader() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", ""))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Token tidak ditemukan"));

        verify(authService, never()).logout(anyString());
    }

    @Test
    void testLogout_Failure() throws Exception {
        doThrow(new RuntimeException("Logout failed")).when(authService).logout("token123");

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer token123"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Logout failed"));
    }

    @Test
    void testSyncOAuthUser_Success() throws Exception {
        Map<String, Object> response = new HashMap<>();
        response.put("access_token", "oauth-token");

        when(authService.syncOAuthUser("oauth-token")).thenReturn(response);

        mockMvc.perform(post("/auth/oauth")
                        .header("Authorization", "Bearer oauth-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("oauth-token"));

        verify(authService, times(1)).syncOAuthUser("oauth-token");
    }

    @Test
    void testSyncOAuthUser_MissingToken() throws Exception {
        mockMvc.perform(post("/auth/oauth"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Token tidak ditemukan"));

        verify(authService, never()).syncOAuthUser(anyString());
    }

    @Test
    void testSyncOAuthUser_Failure() throws Exception {
        when(authService.syncOAuthUser("invalid-token")).thenThrow(new RuntimeException("Gagal sinkronisasi user OAuth"));

        mockMvc.perform(post("/auth/oauth")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Gagal sinkronisasi user OAuth"));
    }
}
