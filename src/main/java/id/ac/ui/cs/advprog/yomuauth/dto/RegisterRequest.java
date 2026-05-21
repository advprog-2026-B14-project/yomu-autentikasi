package id.ac.ui.cs.advprog.yomuauth.dto;

import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
public class RegisterRequest {
    
    @NotBlank(message = "Email tidak boleh kosong")
    @Email(message = "Format email tidak valid")
    private String email;
    
    @NotBlank(message = "Password tidak boleh kosong")
    @Size(min = 8, message = "Password minimal 8 karakter")
    private String password;
    
    @NotBlank(message = "Nama lengkap tidak boleh kosong")
    private String fullName;
    
    @NotBlank(message = "Username tidak boleh kosong")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username hanya boleh berisi huruf, angka, dan underscore")
    private String username;
}
