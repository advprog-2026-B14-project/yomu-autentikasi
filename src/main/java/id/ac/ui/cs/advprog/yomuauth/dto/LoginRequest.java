package id.ac.ui.cs.advprog.yomuauth.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}