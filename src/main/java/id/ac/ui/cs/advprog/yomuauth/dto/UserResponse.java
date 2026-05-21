package id.ac.ui.cs.advprog.yomuauth.dto;

import id.ac.ui.cs.advprog.yomuauth.model.User;
import lombok.Data;

import java.util.UUID;

@Data
public class UserResponse {
    private UUID id;
    private String email;
    private String fullName;
    private String username;
    private String role;
    private java.sql.Timestamp createdAt;

    public UserResponse(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.fullName = user.getFullName();
        this.username = user.getUsername();
        this.role = user.getRole();
        this.createdAt = user.getCreatedAt();
    }
}
