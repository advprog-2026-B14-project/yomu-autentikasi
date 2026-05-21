package id.ac.ui.cs.advprog.yomuauth.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class SupabaseClient {

    private final RestTemplate restTemplate;
    private final String supabaseUrl;
    private final String supabaseKey;
    private final String supabaseServiceKey;

    @Autowired
    public SupabaseClient(
            RestTemplate restTemplate,
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.anon.key}") String supabaseKey,
            @Value("${supabase.service.key}") String supabaseServiceKey) {
        this.restTemplate = restTemplate;
        this.supabaseUrl = supabaseUrl;
        this.supabaseKey = supabaseKey;
        this.supabaseServiceKey = supabaseServiceKey;
    }

    private HttpHeaders createHeaders(String authKey, boolean isJson) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", authKey);
        headers.set("Authorization", "Bearer " + authKey);
        if (isJson) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        return headers;
    }

    public Map<String, Object> signup(String email, String password, Map<String, String> metadata) {
        HttpHeaders headers = createHeaders(supabaseKey, true);

        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);
        body.put("data", metadata);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String url = supabaseUrl + "/auth/v1/signup";

        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Registrasi gagal: respons tidak valid dari Supabase");
        }

        return response.getBody();
    }

    public Map<String, Object> loginWithPassword(String email, String password) {
        HttpHeaders headers = createHeaders(supabaseKey, true);

        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
        String url = supabaseUrl + "/auth/v1/token?grant_type=password";

        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
        return response.getBody();
    }

    public Map<String, Object> getUser(String token) {
        HttpHeaders headers = createHeaders(supabaseServiceKey, false);
        // Using provided token for authorization instead of service key
        headers.set("Authorization", "Bearer " + token);
        
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = supabaseUrl + "/auth/v1/user";

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Token tidak valid");
        }

        return response.getBody();
    }

    public void deleteUser(UUID id) {
        HttpHeaders headers = createHeaders(supabaseServiceKey, false);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = supabaseUrl + "/auth/v1/admin/users/" + id.toString();

        restTemplate.exchange(url, HttpMethod.DELETE, entity, Map.class);
    }

    public void logout(String token) {
        HttpHeaders headers = createHeaders(supabaseKey, false);
        headers.set("Authorization", "Bearer " + token);
        
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = supabaseUrl + "/auth/v1/logout";

        restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
    }

    public void updatePassword(String newPassword, String token) {
        HttpHeaders headers = createHeaders(supabaseKey, true);
        headers.set("Authorization", "Bearer " + token);

        Map<String, String> body = new HashMap<>();
        body.put("password", newPassword);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
        String url = supabaseUrl + "/auth/v1/user";

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.PUT, entity, Map.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Gagal mengubah password di Supabase");
        }
    }
}
