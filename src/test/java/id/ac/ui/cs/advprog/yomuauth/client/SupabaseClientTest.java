package id.ac.ui.cs.advprog.yomuauth.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupabaseClientTest {

    @Mock
    private RestTemplate restTemplate;

    private SupabaseClient supabaseClient;

    private final String supabaseUrl = "https://example.supabase.co";
    private final String supabaseKey = "dummy-anon-key";
    private final String supabaseServiceKey = "dummy-service-key";

    @BeforeEach
    void setUp() {
        supabaseClient = new SupabaseClient(restTemplate, supabaseUrl, supabaseKey, supabaseServiceKey);
    }

    @Test
    void testSignup_Success() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("full_name", "Test User");

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("id", UUID.randomUUID().toString());

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.postForEntity(
                eq(supabaseUrl + "/auth/v1/signup"),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

        Map<String, Object> result = supabaseClient.signup("test@example.com", "password", metadata);

        assertNotNull(result);
        assertEquals(responseBody.get("id"), result.get("id"));

        verify(restTemplate, times(1)).postForEntity(anyString(), argThat((HttpEntity<?> entity) -> {
            HttpHeaders headers = entity.getHeaders();
            return headers.containsKey("apikey") && headers.containsKey("Authorization");
        }), eq(Map.class));
    }

    @Test
    void testSignup_Failure() {
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        when(restTemplate.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

        assertThrows(RuntimeException.class, () -> supabaseClient.signup("test@example.com", "password", new HashMap<>()));
    }

    @Test
    void testLoginWithPassword_Success() {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("access_token", "jwt");

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.postForEntity(
                eq(supabaseUrl + "/auth/v1/token?grant_type=password"),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

        Map<String, Object> result = supabaseClient.loginWithPassword("test@example.com", "password");

        assertNotNull(result);
        assertEquals("jwt", result.get("access_token"));
    }

    @Test
    void testGetUser_Success() {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("id", UUID.randomUUID().toString());

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(supabaseUrl + "/auth/v1/user"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

        Map<String, Object> result = supabaseClient.getUser("token");

        assertNotNull(result);
        assertEquals(responseBody.get("id"), result.get("id"));
    }

    @Test
    void testDeleteUser_Success() {
        UUID id = UUID.randomUUID();

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(HttpStatus.OK);

        when(restTemplate.exchange(
                eq(supabaseUrl + "/auth/v1/admin/users/" + id.toString()),
                eq(HttpMethod.DELETE),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

        assertDoesNotThrow(() -> supabaseClient.deleteUser(id));
    }

    @Test
    void testLogout_Success() {
        ResponseEntity<Void> responseEntity = new ResponseEntity<>(HttpStatus.NO_CONTENT);

        when(restTemplate.exchange(
                eq(supabaseUrl + "/auth/v1/logout"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Void.class)
        )).thenReturn(responseEntity);

        assertDoesNotThrow(() -> supabaseClient.logout("token"));
    }

    @Test
    void testUpdatePassword_Success() {
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(new HashMap<>(), HttpStatus.OK);

        when(restTemplate.exchange(
                eq(supabaseUrl + "/auth/v1/user"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

        assertDoesNotThrow(() -> supabaseClient.updatePassword("newpass", "token"));
    }

    @Test
    void testUpdatePassword_Failure() {
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

        assertThrows(RuntimeException.class, () -> supabaseClient.updatePassword("newpass", "token"));
    }
}
