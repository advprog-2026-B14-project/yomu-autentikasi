package id.ac.ui.cs.advprog.yomuauth.service;

import id.ac.ui.cs.advprog.yomuauth.dto.RegisterRequest;
import id.ac.ui.cs.advprog.yomuauth.dto.LoginRequest;
import id.ac.ui.cs.advprog.yomuauth.model.User;
import id.ac.ui.cs.advprog.yomuauth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.anon.key}")
    private String supabaseKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public User register(RegisterRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", supabaseKey);
        headers.set("Authorization", "Bearer " + supabaseKey);

        Map<String, Object> supabaseBody = new HashMap<>();
        supabaseBody.put("email", request.getEmail());
        supabaseBody.put("password", request.getPassword());

        Map<String, String> metadata = new HashMap<>();
        metadata.put("full_name", request.getFullName());
        metadata.put("username", request.getUsername());
        supabaseBody.put("data", metadata);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(supabaseBody, headers);

        try {
            String url = supabaseUrl + "/auth/v1/signup";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> userMap = (Map<String, Object>) response.getBody().get("user");
                if (userMap == null) {
                    throw new RuntimeException("Supabase tidak mengembalikan data user. Cek apakah email sudah terdaftar.");
                }
                String supabaseId = (String) userMap.get("id");

                User user = new User();
                user.setId(UUID.fromString(supabaseId));
                user.setEmail(request.getEmail());
                user.setFullName(request.getFullName());
                user.setUsername(request.getUsername());
                user.setRole("USER");

                return userRepository.save(user);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Gagal daftar ke Supabase: " + e.getMessage());
        }
        return null;
    }

    public Map<String, Object> login(LoginRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", supabaseKey);
        headers.set("Authorization", "Bearer " + supabaseKey);

        String emailToUse = request.getIdentifier();
        if (!emailToUse.contains("@")) {
            User user = userRepository.findByUsername(emailToUse)
                    .orElseThrow(() -> new RuntimeException("Login gagal: Username tidak ditemukan."));
            emailToUse = user.getEmail();
        }

        Map<String, String> body = new HashMap<>();
        body.put("email", emailToUse);
        body.put("password", request.getPassword());

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        String url = supabaseUrl + "/auth/v1/token?grant_type=password";

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Login gagal: Email/Username atau password salah.");
        }
    }

    public User syncExternalUser(Map<String, Object> supabaseUser) {
        String supabaseId = (String) supabaseUser.get("id");
        String email = (String) supabaseUser.get("email");

        Map<String, Object> metadata = (Map<String, Object>) supabaseUser.get("user_metadata");
        String fullName = (String) metadata.get("full_name");
        String username = (String) metadata.get("username");

        return userRepository.findByEmail(email)
                .map(existingUser -> {
                    existingUser.setFullName(fullName);
                    if (username != null) existingUser.setUsername(username);
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setId(UUID.fromString(supabaseId));
                    newUser.setEmail(email);
                    newUser.setFullName(fullName);
                    newUser.setUsername(username);
                    newUser.setRole("USER");
                    return userRepository.save(newUser);
                });
    }

    @Value("${supabase.service.key}")
    private String supabaseServiceKey;

    public User verifyTokenAndGetUser(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", supabaseServiceKey);
        headers.set("Authorization", "Bearer " + token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    supabaseUrl + "/auth/v1/user",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String supabaseId = (String) response.getBody().get("id");
                return userRepository.findById(UUID.fromString(supabaseId))
                        .orElseThrow(() -> new RuntimeException("User tidak ditemukan di DB lokal"));
            }
        } catch (Exception e) {
            throw new RuntimeException("Token tidak valid");
        }
        throw new RuntimeException("Token tidak valid");
    }

    public void deleteUserFromSupabase(String supabaseId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", supabaseServiceKey);
        headers.set("Authorization", "Bearer " + supabaseServiceKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(
                    supabaseUrl + "/auth/v1/admin/users/" + supabaseId,
                    HttpMethod.DELETE,
                    entity,
                    Map.class
            );
        } catch (Exception e) {
            throw new RuntimeException("Gagal hapus user dari Supabase: " + e.getMessage());
        }
    }
}