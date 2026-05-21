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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    // Metode getAuthenticatedUser manual dihapus karena sekarang digantikan oleh @AuthenticationPrincipal

    @GetMapping("/{id}")
    public ResponseEntity<?> getProfile(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        try {
            User user = userService.getUserById(id);
            return ResponseEntity.ok(new UserResponse(user));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/profile/{id}")
    public ResponseEntity<?> updateProfile(
            @PathVariable UUID id,
            @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal User currentUser) {
        try {
            if (!currentUser.getId().equals(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Akses ditolak: Anda tidak dapat mengubah profil pengguna lain");
            }

            User updatedUser = userService.updateProfile(id, request);
            return ResponseEntity.ok(new UserResponse(updatedUser));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/password")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal User currentUser,
            @RequestHeader(value = "Authorization", required = true) String authHeader) {
        try {
            String token = authHeader.substring(7);

            authService.changePassword(currentUser, request.getOldPassword(), request.getNewPassword(), token);

            return ResponseEntity.ok(Map.of("message", "Password berhasil diubah"));
        } catch (RuntimeException e) {
            if (e.getMessage().equals("Password lama salah")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/account")
    public ResponseEntity<?> deleteAccount(
            @Valid @RequestBody DeleteAccountRequest request,
            @AuthenticationPrincipal User currentUser) {
        try {
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
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}