package id.ac.ui.cs.advprog.yomuauth.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Password lama tidak boleh kosong")
    private String oldPassword;

    @NotBlank(message = "Password baru tidak boleh kosong")
    @Size(min = 8, message = "Password baru minimal 8 karakter")
    private String newPassword;
}
