package id.ac.ui.cs.advprog.yomuauth.service;

import id.ac.ui.cs.advprog.yomuauth.dto.UpdateProfileRequest;
import id.ac.ui.cs.advprog.yomuauth.model.User;
import id.ac.ui.cs.advprog.yomuauth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User updateProfile(UUID id, UpdateProfileRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        if (request.getUsername() != null) {
            userRepository.findByUsername(request.getUsername()).ifPresent(existingUser -> {
                if (!existingUser.getId().equals(id)) {
                    throw new RuntimeException("Username sudah dipakai");
                }
            });
            user.setUsername(request.getUsername());
        }

        return userRepository.save(user);
    }

    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
    }

    public void deleteUser(UUID id) {
        userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
        userRepository.deleteById(id);
    }

    public User updateUserRole(UUID id, String role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

        if (!role.equals("ADMIN") && !role.equals("USER")) {
            throw new RuntimeException("Role tidak valid. Gunakan ADMIN atau USER");
        }

        user.setRole(role);
        return userRepository.save(user);
    }

    public java.util.List<User> getAllUsers() {
        return userRepository.findAll();
    }
}