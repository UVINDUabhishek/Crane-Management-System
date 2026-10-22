package com.uvindu.crane_management_system.service;

import com.uvindu.crane_management_system.BaseUnitTest;
import com.uvindu.crane_management_system.dto.ChangePasswordRequest;
import com.uvindu.crane_management_system.dto.CreateUserRequest;
import com.uvindu.crane_management_system.dto.UpdateUserRequest;
import com.uvindu.crane_management_system.dto.UserResponse;
import com.uvindu.crane_management_system.model.User;
import com.uvindu.crane_management_system.model.UserRole;
import com.uvindu.crane_management_system.repository.UserRepository;
import com.uvindu.crane_management_system.util.UserTestDataBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceTest extends BaseUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldRegisterUserSuccessfully() {

        // Arrange
        CreateUserRequest request = UserTestDataBuilder.createUserRequest(UserRole.SALES);
        User savedUser = UserTestDataBuilder.createUser(1L, UserRole.SALES);

        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedPasswordHash");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        UserResponse result = userService.register(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isNotNull();
        assertThat(result.getEmail()).isNotNull();
        assertThat(result.getRole()).isEqualTo(UserRole.SALES);
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldChangeAdminPasswordSuccessfully() {

        // Arrange
        User admin = UserTestDataBuilder.createUser(1L, UserRole.ADMIN);
        admin.setEmail("admin@example.com");
        ChangePasswordRequest request = UserTestDataBuilder.createChangePasswordRequest();

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("oldPassword", "$2a$10$encodedPasswordHash")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("$2a$10$newEncodedHash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userService.changeAdminPassword("admin@example.com", request);

        // Assert
        verify(passwordEncoder).matches("oldPassword", "$2a$10$encodedPasswordHash");
        verify(passwordEncoder).encode("newPassword123");
        verify(userRepository).save(admin);
    }


    @Test
    void shouldChangeAdminPasswordAndSaveNewPassword() {

        // Arrange
        User admin = UserTestDataBuilder.createUser(1L, UserRole.ADMIN);

        admin.setEmail("admin@example.com");

        ChangePasswordRequest request = UserTestDataBuilder.createChangePasswordRequest();

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));

        when(passwordEncoder.matches(
                "oldPassword",
                admin.getPassword()))
                .thenReturn(true);

        when(passwordEncoder.encode("newPassword123")).thenReturn("$2a$10$newEncodedHash");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userService.changeAdminPassword("admin@example.com", request);

        // Assert
        assertThat(admin.getPassword()).isEqualTo("$2a$10$newEncodedHash");

        verify(userRepository).save(admin);
    }


    @Test
    void shouldThrowExceptionWhenChangingPasswordForNonExistentUser() {

        // Arrange
        ChangePasswordRequest request = UserTestDataBuilder.createChangePasswordRequest();
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.changeAdminPassword("nonexistent@example.com", request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");
    }

    @Test
    void shouldThrowExceptionWhenChangingPasswordForNonAdminUser() {

        // Arrange
        User salesUser = UserTestDataBuilder.createUser(1L, UserRole.SALES);
        salesUser.setEmail("sales@example.com");
        ChangePasswordRequest request = UserTestDataBuilder.createChangePasswordRequest();

        when(userRepository.findByEmail("sales@example.com")).thenReturn(Optional.of(salesUser));

        // Act & Assert
        assertThatThrownBy(() -> userService.changeAdminPassword("sales@example.com", request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Only admin can change the password");
    }

    @Test
    void shouldThrowExceptionWhenCurrentPasswordIsIncorrect() {

        // Arrange
        User admin = UserTestDataBuilder.createUser(1L, UserRole.ADMIN);
        admin.setEmail("admin@example.com");
        ChangePasswordRequest request = UserTestDataBuilder.createChangePasswordRequest();

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("oldPassword", admin.getPassword())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> userService.changeAdminPassword("admin@example.com", request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Current password is incorrect");

        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void shouldRegisterUserWithEncodedPasswordAndCorrectDetails() {

        // Arrange
        CreateUserRequest request = UserTestDataBuilder.createUserRequest(UserRole.SALES);

        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedPasswordHash");

        User savedUser = UserTestDataBuilder.createUser(1L, UserRole.SALES);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        userService.register(request);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(userCaptor.capture());

        User savedEntity = userCaptor.getValue();

        assertThat(savedEntity.getUsername()).isEqualTo("newuser");
        assertThat(savedEntity.getEmail()).isEqualTo("newuser@example.com");
        assertThat(savedEntity.getPassword()).isEqualTo("$2a$10$encodedPasswordHash");
        assertThat(savedEntity.getRole()).isEqualTo(UserRole.SALES);

        verify(passwordEncoder).encode("password123");
    }

    @Test
    void shouldGetAllUsers() {

        // Arrange
        User user1 = UserTestDataBuilder.createUser(1L, UserRole.ADMIN);
        User user2 = UserTestDataBuilder.createUser(2L, UserRole.SALES);
        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));

        // Act
        List<UserResponse> responses = userService.getAllUsers();

        // Assert
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getUsername()).isEqualTo("testuser1");
        assertThat(responses.get(1).getUsername()).isEqualTo("testuser2");
        verify(userRepository).findAll();
    }

    @Test
    void shouldGetUserById() {

        // Arrange
        User user = UserTestDataBuilder.createUser(1L, UserRole.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act
        UserResponse response = userService.getUserById(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("testuser1");
        assertThat(response.getEmail()).isEqualTo("test1@example.com");
        assertThat(response.getRole()).isEqualTo(UserRole.ADMIN);
        verify(userRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenGettingNonExistentUser() {

        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");
    }

    @Test
    void shouldUpdateUser() {

        // Arrange
        User existingUser = UserTestDataBuilder.createUser(1L, UserRole.SALES);
        UpdateUserRequest request = UserTestDataBuilder.createUpdateUserRequest(UserRole.OPERATIONS);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UserResponse response = userService.updateUser(1L, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("updateduser");
        assertThat(response.getEmail()).isEqualTo("updated@example.com");
        assertThat(response.getRole()).isEqualTo(UserRole.OPERATIONS);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentUser() {

        // Arrange
        UpdateUserRequest request = UserTestDataBuilder.createUpdateUserRequest(UserRole.SALES);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUser(999L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");
    }

    @Test
    void shouldDeleteUser() {

        // Arrange
        User user = UserTestDataBuilder.createUser(1L, UserRole.SALES);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userService.deleteUser(1L);

        // Assert
        assertThat(user.isActive()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentUser() {

        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.deleteUser(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");
    }

    @Test
    void shouldThrowExceptionWhenDeletingAdminUser() {

        // Arrange
        User admin = UserTestDataBuilder.createUser(1L, UserRole.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        // Act & Assert
        assertThatThrownBy(() -> userService.deleteUser(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Admin account cannot be deactivated");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldResolveCurrentUserByAuthenticatedEmail() {
        User user = UserTestDataBuilder.createUser(42L, UserRole.SALES);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        UserResponse response = userService.getCurrentUser(user.getEmail());
        assertThat(response.getId()).isEqualTo(42L);
        assertThat(response.getUsername()).isEqualTo(user.getUsername());
        assertThat(response.getEmail()).isEqualTo(user.getEmail());
        assertThat(response.getRole()).isEqualTo(UserRole.SALES);
    }

    @Test
    void shouldRejectMissingCurrentUser() {
        when(userRepository.findByEmail("missing@test.local")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getCurrentUser("missing@test.local"))
                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class);
    }
}
