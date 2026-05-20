package id.ac.ui.cs.advprog.yomuauth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.yomuauth.model.User;
import id.ac.ui.cs.advprog.yomuauth.service.AuthService;
import id.ac.ui.cs.advprog.yomuauth.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AdminController adminController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testGetAllUsers_Success() throws Exception {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");

        Page<User> page = new PageImpl<>(Collections.singletonList(user), PageRequest.of(0, 10), 1);
        when(userService.getAllUsers(anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/admin/users")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("user@example.com"));

        verify(userService, times(1)).getAllUsers(0, 10);
    }

    @Test
    void testDeleteUser_Success() throws Exception {
        UUID userId = UUID.randomUUID();

        doNothing().when(authService).deleteUser(userId);

        mockMvc.perform(delete("/admin/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(content().string("User berhasil dihapus"));

        verify(authService, times(1)).deleteUser(userId);
    }

    @Test
    void testDeleteUser_Failure() throws Exception {
        UUID userId = UUID.randomUUID();

        doThrow(new RuntimeException("Gagal menghapus user")).when(authService).deleteUser(userId);

        mockMvc.perform(delete("/admin/users/" + userId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Gagal menghapus user"));

        verify(authService, times(1)).deleteUser(userId);
    }

    @Test
    void testUpdateRole_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        Map<String, String> body = new HashMap<>();
        body.put("role", "ADMIN");

        User updatedUser = new User();
        updatedUser.setId(userId);
        updatedUser.setEmail("admin@example.com");
        updatedUser.setRole("ADMIN");

        when(userService.updateUserRole(eq(userId), eq("ADMIN"))).thenReturn(updatedUser);

        mockMvc.perform(patch("/admin/users/" + userId + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.email").value("admin@example.com"));

        verify(userService, times(1)).updateUserRole(userId, "ADMIN");
    }

    @Test
    void testUpdateRole_Failure() throws Exception {
        UUID userId = UUID.randomUUID();
        Map<String, String> body = new HashMap<>();
        body.put("role", "INVALID_ROLE");

        when(userService.updateUserRole(eq(userId), eq("INVALID_ROLE")))
                .thenThrow(new RuntimeException("Role tidak valid. Gunakan ADMIN atau USER"));

        mockMvc.perform(patch("/admin/users/" + userId + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Role tidak valid. Gunakan ADMIN atau USER"));

        verify(userService, times(1)).updateUserRole(userId, "INVALID_ROLE");
    }
}
