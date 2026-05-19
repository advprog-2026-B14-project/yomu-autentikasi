package id.ac.ui.cs.advprog.yomuauth.controller;

import id.ac.ui.cs.advprog.yomuauth.dto.UserResponse;
import id.ac.ui.cs.advprog.yomuauth.model.User;
import id.ac.ui.cs.advprog.yomuauth.service.AuthService;
import id.ac.ui.cs.advprog.yomuauth.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @GetMapping("/users")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<UserResponse> responsePage = userService.getAllUsers(page, size).map(UserResponse::new);
        return ResponseEntity.ok(responsePage);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable UUID id) {
        try {
            authService.deleteUser(id);
            return ResponseEntity.ok("User berhasil dihapus");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<?> updateRole(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        try {
            String role = body.get("role");
            User updated = userService.updateUserRole(id, role);
            return ResponseEntity.ok(new UserResponse(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}