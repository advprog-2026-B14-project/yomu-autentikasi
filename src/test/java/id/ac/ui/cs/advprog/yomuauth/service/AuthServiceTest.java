package id.ac.ui.cs.advprog.yomuauth.service;

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
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

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
    private RestTemplate restTemplate;

    private AuthService authService;

    private final String supabaseUrl = "https://example.supabase.co";
    private final String supabaseKey = "dummy-anon-key";
    private final String supabaseServiceKey = "dummy-service-key";

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, restTemplate, supabaseUrl, supabaseKey, supabaseServiceKey);
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

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.postForEntity(
                eq(supabaseUrl + "/auth/v1/signup"),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

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
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

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

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        when(restTemplate.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.register(request);
        });

        assertTrue(exception.getMessage().contains("Gagal daftar ke Supabase"));
    }

    @Test
    void testLogin_Success_WithEmail() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("login@example.com");
        request.setPassword("password123");

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("access_token", "dummy-jwt");
        responseBody.put("expires_in", 3600);

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.postForEntity(
                eq(supabaseUrl + "/auth/v1/token?grant_type=password"),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

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

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(userRepository.findByUsername("loginuser")).thenReturn(Optional.of(user));
        when(restTemplate.postForEntity(
                eq(supabaseUrl + "/auth/v1/token?grant_type=password"),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

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

        when(restTemplate.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenThrow(new RuntimeException("Bad Credentials"));

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

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(supabaseUrl + "/auth/v1/user"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

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

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

        when(userRepository.findById(supabaseId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.verifyTokenAndGetUser(token);
        });

        assertEquals("Token tidak valid", exception.getMessage());
    }

    @Test
    void testVerifyTokenAndGetUser_InvalidToken() {
        String token = "invalid-jwt";

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenThrow(new RuntimeException("Unauthorized"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.verifyTokenAndGetUser(token);
        });

        assertEquals("Token tidak valid", exception.getMessage());
    }

    @Test
    void testDeleteUser_Success() {
        UUID id = UUID.randomUUID();

        when(restTemplate.exchange(
                eq(supabaseUrl + "/auth/v1/admin/users/" + id.toString()),
                eq(HttpMethod.DELETE),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(new ResponseEntity<>(HttpStatus.OK));

        when(userRepository.existsById(id)).thenReturn(true);
        doNothing().when(userRepository).deleteById(id);

        assertDoesNotThrow(() -> authService.deleteUser(id));

        verify(userRepository, times(1)).existsById(id);
        verify(userRepository, times(1)).deleteById(id);
    }

    @Test
    void testDeleteUser_SupabaseFailure() {
        UUID id = UUID.randomUUID();

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.DELETE),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenThrow(new RuntimeException("API error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.deleteUser(id);
        });

        assertTrue(exception.getMessage().contains("Gagal hapus user dari Supabase"));
        verify(userRepository, never()).deleteById(any(UUID.class));
    }

    @Test
    void testLogout_Success() {
        String token = "token";

        when(restTemplate.exchange(
                eq(supabaseUrl + "/auth/v1/logout"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Void.class)
        )).thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        assertDoesNotThrow(() -> authService.logout(token));
    }

    @Test
    void testLogout_Failure() {
        String token = "token";

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Void.class)
        )).thenThrow(new RuntimeException("Failed"));

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

        // Mock the internal login call inside changePassword (which uses login)
        Map<String, Object> loginResponse = new HashMap<>();
        ResponseEntity<Map> loginResponseEntity = new ResponseEntity<>(loginResponse, HttpStatus.OK);
        when(restTemplate.postForEntity(
                eq(supabaseUrl + "/auth/v1/token?grant_type=password"),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(loginResponseEntity);

        // Mock the PUT call to update password
        ResponseEntity<Map> updateResponseEntity = new ResponseEntity<>(new HashMap<>(), HttpStatus.OK);
        when(restTemplate.exchange(
                eq(supabaseUrl + "/auth/v1/user"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(updateResponseEntity);

        assertDoesNotThrow(() -> authService.changePassword(mockUser, oldPassword, newPassword, token));
    }

    @Test
    void testChangePassword_OldPasswordWrong() {
        User mockUser = new User();
        mockUser.setEmail("user@example.com");

        when(restTemplate.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenThrow(new RuntimeException("Wrong password"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.changePassword(mockUser, "wrong", "new", "token");
        });

        assertEquals("Password lama salah", exception.getMessage());
        verify(restTemplate, never()).exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void testChangePassword_PutFailed() {
        User mockUser = new User();
        mockUser.setEmail("user@example.com");

        Map<String, Object> loginResponse = new HashMap<>();
        ResponseEntity<Map> loginResponseEntity = new ResponseEntity<>(loginResponse, HttpStatus.OK);
        when(restTemplate.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(loginResponseEntity);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenThrow(new RuntimeException("Network error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.changePassword(mockUser, "old", "new", "token");
        });

        assertTrue(exception.getMessage().contains("Gagal mengubah password"));
    }
}
