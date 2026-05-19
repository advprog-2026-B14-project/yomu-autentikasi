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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.anon.key}")
    private String supabaseKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

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
                    logger.warn(
                            "Registrasi gagal karena userMap null untuk email: {}",
                            request.getEmail()
                    );
                    throw new RuntimeException(
                            "Registrasi gagal. Email mungkin sudah terdaftar."
                    );
                }

                String supabaseId = (String) userMap.get("id");

                User user = new User();
                user.setId(UUID.fromString(supabaseId));
                user.setEmail(request.getEmail());
                user.setFullName(request.getFullName());
                user.setUsername(request.getUsername());
                user.setRole("USER");

                logger.info(
                        "User berhasil registrasi dengan email: {}",
                        request.getEmail()
                );

                return userRepository.save(user);
            }

            logger.error(
                    "Respons tidak valid dari Supabase saat registrasi email: {}",
                    request.getEmail()
            );

            throw new RuntimeException(
                    "Registrasi gagal: respons tidak valid dari Supabase"
            );

        } catch (Exception e) {
            logger.error("Error saat registrasi user", e);
            throw new RuntimeException("Gagal daftar ke Supabase: " + e.getMessage());
        }
    }

    public Map<String, Object> login(LoginRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", supabaseKey);
        headers.set("Authorization", "Bearer " + supabaseKey);

        String emailToUse = request.getIdentifier();
        if (!emailToUse.contains("@")) {
            User user = userRepository.findByUsername(emailToUse)
                    .orElseThrow(() -> new RuntimeException("Login gagal: Kredensial tidak valid."));
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
            throw new RuntimeException("Login gagal: Kredensial tidak valid.");
        }
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

    public void logout(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", supabaseKey);
        headers.set("Authorization", "Bearer " + token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = supabaseUrl + "/auth/v1/logout";

        try {
            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
        } catch (Exception e) {
            throw new RuntimeException("Logout gagal: " + e.getMessage());
        }
    }

    public void changePassword(User user, String oldPassword, String newPassword, String token) {
        LoginRequest loginReq = new LoginRequest();
        loginReq.setIdentifier(user.getEmail());
        loginReq.setPassword(oldPassword);

        try {
            login(loginReq);
        } catch (Exception e) {
            throw new RuntimeException("Password lama salah");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", supabaseKey);
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("password", newPassword);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
        String url = supabaseUrl + "/auth/v1/user";

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.PUT, entity, Map.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Gagal mengubah password di Supabase");
            }
        } catch (Exception e) {
            throw new RuntimeException("Gagal mengubah password: " + e.getMessage());
        }
    }
}