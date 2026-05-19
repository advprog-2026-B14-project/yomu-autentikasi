package id.ac.ui.cs.advprog.yomuauth.controller;

import id.ac.ui.cs.advprog.yomuauth.dto.ChangePasswordRequest;
import id.ac.ui.cs.advprog.yomuauth.dto.DeleteAccountRequest;
import id.ac.ui.cs.advprog.yomuauth.dto.LoginRequest;
import id.ac.ui.cs.advprog.yomuauth.dto.UpdateProfileRequest;
import id.ac.ui.cs.advprog.yomuauth.dto.UserResponse;
import id.ac.ui.cs.advprog.yomuauth.model.User;
import id.ac.ui.cs.advprog.yomuauth.service.AuthService;
import id.ac.ui.cs.advprog.yomuauth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    private User getAuthenticatedUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Unauthorized");
        }
        String token = authHeader.substring(7);
        return authService.verifyTokenAndGetUser(token);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProfile(
            @PathVariable UUID id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User currentUser = getAuthenticatedUser(authHeader);
            if (!currentUser.getId().equals(id) && !"ADMIN".equals(currentUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Akses ditolak: Anda bukan pemilik profil ini");
            }

            User user = userService.getUserById(id);
            return ResponseEntity.ok(new UserResponse(user));
        } catch (RuntimeException e) {
            if (e.getMessage().equals("Unauthorized") || e.getMessage().contains("tidak valid")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token tidak valid atau tidak ditemukan");
            }
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/profile/{id}")
    public ResponseEntity<?> updateProfile(
            @PathVariable UUID id,
            @RequestBody UpdateProfileRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User currentUser = getAuthenticatedUser(authHeader);
            if (!currentUser.getId().equals(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Akses ditolak: Anda tidak dapat mengubah profil pengguna lain");
            }

            User updatedUser = userService.updateProfile(id, request);
            return ResponseEntity.ok(new UserResponse(updatedUser));
        } catch (RuntimeException e) {
            if (e.getMessage().equals("Unauthorized") || e.getMessage().contains("tidak valid")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token tidak valid atau tidak ditemukan");
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/password")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User currentUser = getAuthenticatedUser(authHeader);
            String token = authHeader.substring(7);

            authService.changePassword(currentUser, request.getOldPassword(), request.getNewPassword(), token);

            return ResponseEntity.ok(Map.of("message", "Password berhasil diubah"));
        } catch (RuntimeException e) {
            if (e.getMessage().equals("Unauthorized") || e.getMessage().contains("tidak valid")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token tidak valid atau tidak ditemukan");
            }
            if (e.getMessage().equals("Password lama salah")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/account")
    public ResponseEntity<?> deleteAccount(
            @Valid @RequestBody DeleteAccountRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User currentUser = getAuthenticatedUser(authHeader);

            LoginRequest loginReq = new LoginRequest();
            loginReq.setIdentifier(currentUser.getEmail());
            loginReq.setPassword(request.getPassword());
            try {
                authService.login(loginReq);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Password salah");
            }

            authService.deleteUser(currentUser.getId());

            return ResponseEntity.ok(Map.of("message", "Akun berhasil dihapus"));
        } catch (RuntimeException e) {
            if (e.getMessage() != null
                    && (e.getMessage().equals("Unauthorized") || e.getMessage().contains("tidak valid"))) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token tidak valid atau tidak ditemukan");
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}