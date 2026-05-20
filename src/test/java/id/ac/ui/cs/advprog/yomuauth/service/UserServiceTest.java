package id.ac.ui.cs.advprog.yomuauth.service;

import id.ac.ui.cs.advprog.yomuauth.dto.UpdateProfileRequest;
import id.ac.ui.cs.advprog.yomuauth.model.User;
import id.ac.ui.cs.advprog.yomuauth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setEmail("test@example.com");
        user.setFullName("Test User");
        user.setUsername("testuser");
        user.setRole("USER");

        userService = new UserServiceImpl(userRepository);
    }

    @Test
    void testGetUserById_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        User result = userService.getUserById(userId);

        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void testGetUserById_UserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.getUserById(userId);
        });

        assertEquals("User tidak ditemukan", exception.getMessage());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void testUpdateProfile_Success_AllFields() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Updated Name");
        request.setUsername("updateduser");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("updateduser")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateProfile(userId, request);

        assertNotNull(result);
        assertEquals("Updated Name", result.getFullName());
        assertEquals("updateduser", result.getUsername());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void testUpdateProfile_Success_UsernameAlreadyOwnedBySelf() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setUsername("testuser");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateProfile(userId, request);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void testUpdateProfile_UsernameAlreadyTakenByOther() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setUsername("takenuser");

        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        otherUser.setUsername("takenuser");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("takenuser")).thenReturn(Optional.of(otherUser));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.updateProfile(userId, request);
        });

        assertEquals("Username sudah dipakai", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testUpdateProfile_UserNotFound() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.updateProfile(userId, request);
        });

        assertEquals("User tidak ditemukan", exception.getMessage());
    }

    @Test
    void testDeleteUser_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        doNothing().when(userRepository).deleteById(userId);

        assertDoesNotThrow(() -> userService.deleteUser(userId));

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).deleteById(userId);
    }

    @Test
    void testDeleteUser_UserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.deleteUser(userId);
        });

        assertEquals("User tidak ditemukan", exception.getMessage());
        verify(userRepository, never()).deleteById(any(UUID.class));
    }

    @Test
    void testUpdateUserRole_Success_ToAdmin() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateUserRole(userId, "ADMIN");

        assertNotNull(result);
        assertEquals("ADMIN", result.getRole());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void testUpdateUserRole_Success_ToUser() {
        user.setRole("ADMIN");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateUserRole(userId, "USER");

        assertNotNull(result);
        assertEquals("USER", result.getRole());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void testUpdateUserRole_InvalidRole() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.updateUserRole(userId, "SUPERADMIN");
        });

        assertEquals("Role tidak valid. Gunakan ADMIN atau USER", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testUpdateUserRole_UserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.updateUserRole(userId, "ADMIN");
        });

        assertEquals("User tidak ditemukan", exception.getMessage());
    }

    @Test
    void testGetAllUsers() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(Collections.singletonList(user));
        when(userRepository.findAll(pageRequest)).thenReturn(page);

        Page<User> result = userService.getAllUsers(0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(user, result.getContent().get(0));
        verify(userRepository, times(1)).findAll(pageRequest);
    }
}
