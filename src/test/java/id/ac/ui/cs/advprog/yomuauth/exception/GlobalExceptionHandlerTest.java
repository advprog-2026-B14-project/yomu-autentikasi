package id.ac.ui.cs.advprog.yomuauth.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.yomuauth.controller.AuthController;
import id.ac.ui.cs.advprog.yomuauth.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthController authController; // Needed to set up MockMvc standalone context

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(globalExceptionHandler)
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testHandleValidationExceptions() throws Exception {
        // Send a request with blank values to trigger MethodArgumentNotValidException
        RegisterRequest invalidRequest = new RegisterRequest();
        invalidRequest.setEmail("");
        invalidRequest.setPassword("short");
        invalidRequest.setFullName("");
        invalidRequest.setUsername(null);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("Email tidak boleh kosong"))
                .andExpect(jsonPath("$.password").value("Password minimal 8 karakter"))
                .andExpect(jsonPath("$.fullName").value("Nama lengkap tidak boleh kosong"))
                .andExpect(jsonPath("$.username").value("Username tidak boleh kosong"));
    }
}
