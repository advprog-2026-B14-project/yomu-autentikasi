package id.ac.ui.cs.advprog.yomuauth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.yomuauth.dto.ChangePasswordRequest;
import id.ac.ui.cs.advprog.yomuauth.dto.DeleteAccountRequest;
import id.ac.ui.cs.advprog.yomuauth.dto.UpdateProfileRequest;
import id.ac.ui.cs.advprog.yomuauth.model.User;
import id.ac.ui.cs.advprog.yomuauth.service.AuthService;
import id.ac.ui.cs.advprog.yomuauth.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @Mock
    private AuthService authService;

    @InjectMocks
    private UserController userController;

    private User currentUser;
    private UUID currentUserId;

    @BeforeEach
    void setUp() {
        currentUserId = UUID.randomUUID();
        currentUser = new User();
        currentUser.setId(currentUserId);
        currentUser.setEmail("current@example.com");
        currentUser.setRole("USER");
        currentUser.setUsername("currentuser");

        // Set up MockMvc with a custom argument resolver to inject the @AuthenticationPrincipal
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType().equals(User.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return currentUser;
                    }
                })
                .build();
    }

    @Test
    void testGetProfile_Success_Owner() throws Exception {
        User user = new User();
        user.setId(currentUserId);
        user.setEmail("current@example.com");

        when(userService.getUserById(currentUserId)).thenReturn(user);

        mockMvc.perform(get("/user/" + currentUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("current@example.com"));

        verify(userService, times(1)).getUserById(currentUserId);
    }

    @Test
    void testGetProfile_Success_Admin() throws Exception {
        currentUser.setRole("ADMIN"); // current user is admin
        UUID otherUserId = UUID.randomUUID();

        User otherUser = new User();
        otherUser.setId(otherUserId);
        otherUser.setEmail("other@example.com");

        when(userService.getUserById(otherUserId)).thenReturn(otherUser);

        mockMvc.perform(get("/user/" + otherUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("other@example.com"));

        verify(userService, times(1)).getUserById(otherUserId);
    }

    @Test
    void testGetProfile_Success_OtherUser() throws Exception {
        UUID otherUserId = UUID.randomUUID();

        User otherUser = new User();
        otherUser.setId(otherUserId);
        otherUser.setEmail("other@example.com");

        when(userService.getUserById(otherUserId)).thenReturn(otherUser);

        mockMvc.perform(get("/user/" + otherUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("other@example.com"));

        verify(userService, times(1)).getUserById(otherUserId);
    }

    @Test
    void testGetProfile_NotFound() throws Exception {
        when(userService.getUserById(currentUserId)).thenThrow(new RuntimeException("Not found"));

        mockMvc.perform(get("/user/" + currentUserId))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetProfileByUsername_Success() throws Exception {
        User user = new User();
        user.setId(currentUserId);
        user.setUsername("currentuser");
        user.setEmail("current@example.com");

        when(userService.getUserByUsername("currentuser")).thenReturn(user);

        mockMvc.perform(get("/user/username/currentuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("currentuser"));

        verify(userService, times(1)).getUserByUsername("currentuser");
    }

    @Test
    void testGetProfileByUsername_NotFound() throws Exception {
        when(userService.getUserByUsername("unknown")).thenThrow(new RuntimeException("User tidak ditemukan"));

        mockMvc.perform(get("/user/username/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateProfile_Success_Owner() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("New Name");

        User updatedUser = new User();
        updatedUser.setId(currentUserId);
        updatedUser.setFullName("New Name");

        when(userService.updateProfile(eq(currentUserId), any(UpdateProfileRequest.class))).thenReturn(updatedUser);

        mockMvc.perform(patch("/user/profile/" + currentUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("New Name"));
    }

    @Test
    void testUpdateProfile_Forbidden_NotOwner() throws Exception {
        UUID otherUserId = UUID.randomUUID();
        UpdateProfileRequest request = new UpdateProfileRequest();

        mockMvc.perform(patch("/user/profile/" + otherUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Akses ditolak: Anda tidak dapat mengubah profil pengguna lain"));

        verify(userService, never()).updateProfile(any(UUID.class), any(UpdateProfileRequest.class));
    }

    @Test
    void testUpdateProfile_BadRequest_Exception() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        when(userService.updateProfile(eq(currentUserId), any(UpdateProfileRequest.class)))
                .thenThrow(new RuntimeException("Username sudah dipakai"));

        mockMvc.perform(patch("/user/profile/" + currentUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Username sudah dipakai"));
    }

    @Test
    void testChangePassword_Success() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("oldPass");
        request.setNewPassword("newPassword");

        doNothing().when(authService).changePassword(eq(currentUser), eq("oldPass"), eq("newPassword"), eq("token123"));

        mockMvc.perform(patch("/user/password")
                        .header("Authorization", "Bearer token123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password berhasil diubah"));
    }

    @Test
    void testChangePassword_OldPasswordWrong() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("wrongPass");
        request.setNewPassword("newPassword");

        doThrow(new RuntimeException("Password lama salah"))
                .when(authService).changePassword(eq(currentUser), eq("wrongPass"), eq("newPassword"), eq("token123"));

        mockMvc.perform(patch("/user/password")
                        .header("Authorization", "Bearer token123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Password lama salah"));
    }

    @Test
    void testChangePassword_GenericError() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("oldPass");
        request.setNewPassword("newPassword");

        doThrow(new RuntimeException("Gagal mengubah password"))
                .when(authService).changePassword(eq(currentUser), eq("oldPass"), eq("newPassword"), eq("token123"));

        mockMvc.perform(patch("/user/password")
                        .header("Authorization", "Bearer token123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Gagal mengubah password"));
    }

    @Test
    void testDeleteAccount_Success() throws Exception {
        DeleteAccountRequest request = new DeleteAccountRequest();
        request.setPassword("correctPassword");

        when(authService.login(any())).thenReturn(null);
        doNothing().when(authService).deleteUser(currentUserId);

        mockMvc.perform(delete("/user/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Akun berhasil dihapus"));

        verify(authService, times(1)).login(any());
        verify(authService, times(1)).deleteUser(currentUserId);
    }

    @Test
    void testDeleteAccount_WrongPassword() throws Exception {
        DeleteAccountRequest request = new DeleteAccountRequest();
        request.setPassword("wrongPassword");

        when(authService.login(any())).thenThrow(new RuntimeException("Wrong password"));

        mockMvc.perform(delete("/user/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Password salah"));

        verify(authService, never()).deleteUser(any(UUID.class));
    }

    @Test
    void testDeleteAccount_GenericException() throws Exception {
        DeleteAccountRequest request = new DeleteAccountRequest();
        request.setPassword("correctPassword");

        when(authService.login(any())).thenReturn(null);
        doThrow(new RuntimeException("Delete failed")).when(authService).deleteUser(currentUserId);

        mockMvc.perform(delete("/user/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Delete failed"));
    }
}
