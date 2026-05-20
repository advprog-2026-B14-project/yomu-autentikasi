package id.ac.ui.cs.advprog.yomuauth.service;

import id.ac.ui.cs.advprog.yomuauth.client.SupabaseClient;
import id.ac.ui.cs.advprog.yomuauth.dto.LoginRequest;
import id.ac.ui.cs.advprog.yomuauth.dto.RegisterRequest;
import id.ac.ui.cs.advprog.yomuauth.model.User;
import id.ac.ui.cs.advprog.yomuauth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final SupabaseClient supabaseClient;

    @Autowired
    public AuthServiceImpl(UserRepository userRepository, SupabaseClient supabaseClient) {
        this.userRepository = userRepository;
        this.supabaseClient = supabaseClient;
    }

    @Override
    public User register(RegisterRequest request) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("full_name", request.getFullName());
            metadata.put("username", request.getUsername());

            Map<String, Object> response = supabaseClient.signup(request.getEmail(), request.getPassword(), metadata);
            Map<String, Object> userMap = (Map<String, Object>) response.get("user");

            if (userMap == null) {
                logger.warn(
                        "Registrasi gagal karena userMap null untuk email: {}",
                        request.getEmail().replaceAll("[\n\r\t]", "_")
                );
                throw new RuntimeException("Registrasi gagal. Email mungkin sudah terdaftar.");
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
                    request.getEmail().replaceAll("[\n\r\t]", "_")
            );

            return userRepository.save(user);

        } catch (Exception e) {
            logger.error("Error saat registrasi user", e);
            if (e.getMessage() != null && e.getMessage().contains("mungkin sudah terdaftar")) {
                throw new RuntimeException("Registrasi gagal. Email mungkin sudah terdaftar.");
            } else if (e.getMessage() != null && e.getMessage().contains("respons tidak valid dari Supabase")) {
                logger.error(
                        "Respons tidak valid dari Supabase saat registrasi email: {}",
                        request.getEmail().replaceAll("[\n\r\t]", "_")
                );
                throw new RuntimeException("Registrasi gagal: respons tidak valid dari Supabase");
            }
            throw new RuntimeException("Gagal daftar ke Supabase: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> login(LoginRequest request) {
        String emailToUse = request.getIdentifier();
        if (!emailToUse.contains("@")) {
            User user = userRepository.findByUsername(emailToUse)
                    .orElseThrow(() -> new RuntimeException("Login gagal: Kredensial tidak valid."));
            emailToUse = user.getEmail();
        }

        try {
            return supabaseClient.loginWithPassword(emailToUse, request.getPassword());
        } catch (Exception e) {
            throw new RuntimeException("Login gagal: Kredensial tidak valid.");
        }
    }

    @Override
    public User verifyTokenAndGetUser(String token) {
        try {
            Map<String, Object> response = supabaseClient.getUser(token);
            String supabaseId = (String) response.get("id");
            return userRepository.findById(UUID.fromString(supabaseId))
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan di DB lokal"));
        } catch (Exception e) {
            throw new RuntimeException("Token tidak valid");
        }
    }

    @Override
    public void deleteUser(UUID id) {
        try {
            supabaseClient.deleteUser(id);
        } catch (Exception e) {
            throw new RuntimeException("Gagal hapus user dari Supabase: " + e.getMessage());
        }

        try {
            if (userRepository.existsById(id)) {
                userRepository.deleteById(id);
            }
        } catch (Exception ignored) {
            // Abaikan jika ternyata baris sudah terhapus oleh trigger
        }
    }

    @Override
    public void logout(String token) {
        try {
            supabaseClient.logout(token);
        } catch (Exception e) {
            throw new RuntimeException("Logout gagal: " + e.getMessage());
        }
    }

    @Override
    public void changePassword(User user, String oldPassword, String newPassword, String token) {
        try {
            supabaseClient.loginWithPassword(user.getEmail(), oldPassword);
        } catch (Exception e) {
            throw new RuntimeException("Password lama salah");
        }

        try {
            supabaseClient.updatePassword(newPassword, token);
        } catch (Exception e) {
            throw new RuntimeException("Gagal mengubah password: " + e.getMessage());
        }
    }
}
