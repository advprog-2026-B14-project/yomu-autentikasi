package id.ac.ui.cs.advprog.yomuauth.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class DeleteAccountRequest {

    @NotBlank(message = "Password tidak boleh kosong")
    private String password;
}
