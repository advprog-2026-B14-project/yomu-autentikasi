package id.ac.ui.cs.advprog.yomuauth.service;

import id.ac.ui.cs.advprog.yomuauth.client.SupabaseClient;
import id.ac.ui.cs.advprog.yomuauth.dto.LoginRequest;
import id.ac.ui.cs.advprog.yomuauth.dto.RegisterRequest;
import id.ac.ui.cs.advprog.yomuauth.model.User;
import id.ac.ui.cs.advprog.yomuauth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SupabaseClient supabaseClient;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, supabaseClient);
    }

    @Test
    void testRegister_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("register@example.com");
        request.setPassword("password123");
        request.setFullName("Register User");
        request.setUsername("registeruser");

        UUID generatedId = UUID.randomUUID();

        Map<String, Object> responseBody = new HashMap<>();
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", generatedId.toString());
        responseBody.put("user", userMap);

        when(supabaseClient.signup(eq("register@example.com"), eq("password123"), anyMap()))
                .thenReturn(responseBody);

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = authService.register(request);

        assertNotNull(result);
        assertEquals(generatedId, result.getId());
        assertEquals("register@example.com", result.getEmail());
        assertEquals("Register User", result.getFullName());
        assertEquals("registeruser", result.getUsername());
        assertEquals("USER", result.getRole());

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegister_Failure_UserMapNull() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("register@example.com");

        Map<String, Object> responseBody = new HashMap<>(); // user is missing (null)

        when(supabaseClient.signup(eq("register@example.com"), any(), anyMap()))
                .thenReturn(responseBody);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.register(request);
        });

        assertTrue(exception.getMessage().contains("Registrasi gagal. Email mungkin sudah terdaftar"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegister_Failure_BadResponse() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("register@example.com");

        when(supabaseClient.signup(anyString(), any(), anyMap()))
                .thenThrow(new RuntimeException("Registrasi gagal: respons tidak valid dari Supabase"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.register(request);
        });

        assertTrue(exception.getMessage().contains("respons tidak valid dari Supabase"));
    }

    @Test
    void testLogin_Success_WithEmail() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("login@example.com");
        request.setPassword("password123");

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("access_token", "dummy-jwt");
        responseBody.put("expires_in", 3600);

        when(supabaseClient.loginWithPassword(eq("login@example.com"), eq("password123")))
                .thenReturn(responseBody);

        Map<String, Object> result = authService.login(request);

        assertNotNull(result);
        assertEquals("dummy-jwt", result.get("access_token"));
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    void testLogin_Success_WithUsername() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("loginuser");
        request.setPassword("password123");

        User user = new User();
        user.setEmail("login@example.com");
        user.setUsername("loginuser");

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("access_token", "dummy-jwt");

        when(userRepository.findByUsername("loginuser")).thenReturn(Optional.of(user));
        
        when(supabaseClient.loginWithPassword(eq("login@example.com"), eq("password123")))
                .thenReturn(responseBody);

        Map<String, Object> result = authService.login(request);

        assertNotNull(result);
        assertEquals("dummy-jwt", result.get("access_token"));
        verify(userRepository, times(1)).findByUsername("loginuser");
    }

    @Test
    void testLogin_Failure_UsernameNotFound() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("loginuser");

        when(userRepository.findByUsername("loginuser")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login(request);
        });

        assertEquals("Login gagal: Kredensial tidak valid.", exception.getMessage());
    }

    @Test
    void testLogin_Failure_InvalidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("login@example.com");

        when(supabaseClient.loginWithPassword(anyString(), any()))
                .thenThrow(new RuntimeException("Bad Credentials"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login(request);
        });

        assertEquals("Login gagal: Kredensial tidak valid.", exception.getMessage());
    }

    @Test
    void testVerifyTokenAndGetUser_Success() {
        String token = "valid-jwt";
        UUID supabaseId = UUID.randomUUID();

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("id", supabaseId.toString());

        when(supabaseClient.getUser(token)).thenReturn(responseBody);

        User mockUser = new User();
        mockUser.setId(supabaseId);
        mockUser.setEmail("user@example.com");

        when(userRepository.findById(supabaseId)).thenReturn(Optional.of(mockUser));

        User result = authService.verifyTokenAndGetUser(token);

        assertNotNull(result);
        assertEquals(supabaseId, result.getId());
    }

    @Test
    void testVerifyTokenAndGetUser_UserNotInLocalDb() {
        String token = "valid-jwt";
        UUID supabaseId = UUID.randomUUID();

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("id", supabaseId.toString());

        when(supabaseClient.getUser(token)).thenReturn(responseBody);
        when(userRepository.findById(supabaseId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.verifyTokenAndGetUser(token);
        });

        assertEquals("Token tidak valid", exception.getMessage());
    }

    @Test
    void testVerifyTokenAndGetUser_InvalidToken() {
        String token = "invalid-jwt";

        when(supabaseClient.getUser(token)).thenThrow(new RuntimeException("Unauthorized"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.verifyTokenAndGetUser(token);
        });

        assertEquals("Token tidak valid", exception.getMessage());
    }

    @Test
    void testDeleteUser_Success() {
        UUID id = UUID.randomUUID();

        doNothing().when(supabaseClient).deleteUser(id);
        when(userRepository.existsById(id)).thenReturn(true);
        doNothing().when(userRepository).deleteById(id);

        assertDoesNotThrow(() -> authService.deleteUser(id));

        verify(userRepository, times(1)).existsById(id);
        verify(userRepository, times(1)).deleteById(id);
    }

    @Test
    void testDeleteUser_SupabaseFailure() {
        UUID id = UUID.randomUUID();

        doThrow(new RuntimeException("API error")).when(supabaseClient).deleteUser(id);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.deleteUser(id);
        });

        assertTrue(exception.getMessage().contains("Gagal hapus user dari Supabase"));
        verify(userRepository, never()).deleteById(any(UUID.class));
    }

    @Test
    void testLogout_Success() {
        String token = "token";

        doNothing().when(supabaseClient).logout(token);

        assertDoesNotThrow(() -> authService.logout(token));
    }

    @Test
    void testLogout_Failure() {
        String token = "token";

        doThrow(new RuntimeException("Failed")).when(supabaseClient).logout(token);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.logout(token);
        });

        assertTrue(exception.getMessage().contains("Logout gagal"));
    }

    @Test
    void testChangePassword_Success() {
        User mockUser = new User();
        mockUser.setEmail("user@example.com");

        String oldPassword = "old";
        String newPassword = "new";
        String token = "token";

        when(supabaseClient.loginWithPassword(eq("user@example.com"), eq("old")))
                .thenReturn(new HashMap<>());
                
        doNothing().when(supabaseClient).updatePassword(eq("new"), eq("token"));

        assertDoesNotThrow(() -> authService.changePassword(mockUser, oldPassword, newPassword, token));
    }

    @Test
    void testChangePassword_OldPasswordWrong() {
        User mockUser = new User();
        mockUser.setEmail("user@example.com");

        when(supabaseClient.loginWithPassword(anyString(), anyString()))
                .thenThrow(new RuntimeException("Wrong password"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.changePassword(mockUser, "wrong", "new", "token");
        });

        assertEquals("Password lama salah", exception.getMessage());
        verify(supabaseClient, never()).updatePassword(anyString(), anyString());
    }

    @Test
    void testChangePassword_PutFailed() {
        User mockUser = new User();
        mockUser.setEmail("user@example.com");

        when(supabaseClient.loginWithPassword(anyString(), anyString()))
                .thenReturn(new HashMap<>());

        doThrow(new RuntimeException("Network error")).when(supabaseClient).updatePassword(anyString(), anyString());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.changePassword(mockUser, "old", "new", "token");
        });

        assertTrue(exception.getMessage().contains("Gagal mengubah password"));
    }
}
