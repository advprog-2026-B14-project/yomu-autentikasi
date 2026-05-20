package id.ac.ui.cs.advprog.yomuauth.service;

import id.ac.ui.cs.advprog.yomuauth.dto.UpdateProfileRequest;
import id.ac.ui.cs.advprog.yomuauth.model.User;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface UserService {
    User updateProfile(UUID id, UpdateProfileRequest request);
    User getUserById(UUID id);
    void deleteUser(UUID id);
    User updateUserRole(UUID id, String role);
    Page<User> getAllUsers(int page, int size);
}