package id.ac.ui.cs.advprog.yomuauth.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class LoginRequest {

    @NotBlank(message = "Email/Username tidak boleh kosong")
    private String identifier;

    @NotBlank(message = "Password tidak boleh kosong")
    private String password;
}