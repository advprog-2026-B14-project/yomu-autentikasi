package id.ac.ui.cs.advprog.yomuauth.service;

import id.ac.ui.cs.advprog.yomuauth.dto.LoginRequest;
import id.ac.ui.cs.advprog.yomuauth.dto.RegisterRequest;
import id.ac.ui.cs.advprog.yomuauth.model.User;

import java.util.Map;
import java.util.UUID;

public interface AuthService {
    User register(RegisterRequest request);
    Map<String, Object> login(LoginRequest request);
    User verifyTokenAndGetUser(String token);
    void deleteUser(UUID id);
    void logout(String token);
    void changePassword(User user, String oldPassword, String newPassword, String token);
    Map<String, Object> syncOAuthUser(String token);
}