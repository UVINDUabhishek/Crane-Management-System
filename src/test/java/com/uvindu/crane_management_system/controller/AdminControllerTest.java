package com.uvindu.crane_management_system.controller;

import com.uvindu.crane_management_system.dto.ChangePasswordRequest;
import com.uvindu.crane_management_system.dto.CreateOperatorRequest;
import com.uvindu.crane_management_system.dto.CreateUserRequest;
import com.uvindu.crane_management_system.dto.OperatorResponse;
import com.uvindu.crane_management_system.dto.UpdateUserRequest;
import com.uvindu.crane_management_system.dto.UserResponse;
import com.uvindu.crane_management_system.model.UserRole;
import com.uvindu.crane_management_system.service.OperatorService;
import com.uvindu.crane_management_system.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private OperatorService operatorService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AdminController adminController;


    // ---------------------------------------------------------
    // Change Admin Password
    // ---------------------------------------------------------

    @Test
    void shouldChangeAdminPassword() {

        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("newPassword");

        when(authentication.getName())
                .thenReturn("admin@example.com");

        // Act
        ResponseEntity<Void> response =
                adminController.changePassword(request, authentication);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(response.getBody()).isNull();

        verify(authentication).getName();

        verify(userService).changeAdminPassword(
                "admin@example.com",
                request
        );
    }


    // ---------------------------------------------------------
    // Create User
    // ---------------------------------------------------------

    @Test
    void shouldCreateUser() {

        // Arrange
        CreateUserRequest request = new CreateUserRequest();

        request.setUsername("admin02");
        request.setEmail("admin02@example.com");
        request.setPassword("password123");
        request.setRole(UserRole.ADMIN);

        UserResponse createdUser = new UserResponse();

        when(userService.register(request))
                .thenReturn(createdUser);

        // Act
        ResponseEntity<UserResponse> response =
                adminController.createUser(request);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(createdUser);

        verify(userService).register(request);
    }


    // ---------------------------------------------------------
    // Get All Users
    // ---------------------------------------------------------

    @Test
    void shouldGetAllUsers() {

        // Arrange
        UserResponse user1 = new UserResponse();
        user1.setUsername("admin01");
        user1.setEmail("admin01@example.com");
        user1.setRole(UserRole.ADMIN);

        UserResponse user2 = new UserResponse();
        user2.setUsername("sales01");
        user2.setEmail("sales01@example.com");
        user2.setRole(UserRole.SALES);

        List<UserResponse> users =
                List.of(user1, user2);

        when(userService.getAllUsers())
                .thenReturn(users);

        // Act
        ResponseEntity<List<UserResponse>> response =
                adminController.getAllUsers();

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(users);
        assertThat(response.getBody()).hasSize(2);

        verify(userService).getAllUsers();
    }


    // ---------------------------------------------------------
    // Get User By ID
    // ---------------------------------------------------------

    @Test
    void shouldGetUserById() {

        // Arrange
        Long userId = 1L;

        UserResponse userResponse = new UserResponse();
        userResponse.setUsername("admin01");
        userResponse.setEmail("admin01@example.com");
        userResponse.setRole(UserRole.ADMIN);

        when(userService.getUserById(userId))
                .thenReturn(userResponse);

        // Act
        ResponseEntity<UserResponse> response =
                adminController.getUserById(userId);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(userResponse);

        verify(userService).getUserById(userId);
    }


    // ---------------------------------------------------------
    // Update User
    // ---------------------------------------------------------

    @Test
    void shouldUpdateUser() {

        // Arrange
        Long userId = 1L;

        UpdateUserRequest request = new UpdateUserRequest();

        request.setUsername("updatedAdmin");
        request.setEmail("updated@example.com");
        request.setRole(UserRole.ADMIN);

        UserResponse updatedUser = new UserResponse();

        updatedUser.setUsername("updatedAdmin");
        updatedUser.setEmail("updated@example.com");
        updatedUser.setRole(UserRole.ADMIN);

        when(userService.updateUser(userId, request))
                .thenReturn(updatedUser);

        // Act
        ResponseEntity<UserResponse> response =
                adminController.updateUser(userId, request);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(updatedUser);

        verify(userService).updateUser(userId, request);
    }


    // ---------------------------------------------------------
    // Delete User
    // ---------------------------------------------------------

    @Test
    void shouldDeleteUser() {

        // Arrange
        long userId = 1L;

        // Act
        ResponseEntity<Void> response =
                adminController.deleteUser(userId);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(response.getBody()).isNull();

        verify(userService).deleteUser(userId);
    }


    // ---------------------------------------------------------
    // Create Operator
    // ---------------------------------------------------------

    @Test
    void shouldCreateOperator() {

        // Arrange
        CreateOperatorRequest request =
                new CreateOperatorRequest();

        request.setOperatorName("John Operator");

        OperatorResponse operatorResponse =
                new OperatorResponse();

        operatorResponse.setId(1L);
        operatorResponse.setOperatorName("John Operator");

        when(operatorService.createOperator(request))
                .thenReturn(operatorResponse);

        // Act
        ResponseEntity<OperatorResponse> response =
                adminController.createOperator(request);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(operatorResponse);

        verify(operatorService).createOperator(request);
    }


    // ---------------------------------------------------------
    // Get All Operators
    // ---------------------------------------------------------

    @Test
    void shouldGetAllOperators() {

        // Arrange
        OperatorResponse operator1 =
                new OperatorResponse();

        operator1.setId(1L);
        operator1.setOperatorName("John Operator");

        OperatorResponse operator2 =
                new OperatorResponse();

        operator2.setId(2L);
        operator2.setOperatorName("David Operator");

        List<OperatorResponse> operators =
                List.of(operator1, operator2);

        when(operatorService.getAllOperators())
                .thenReturn(operators);

        // Act
        ResponseEntity<List<OperatorResponse>> response =
                adminController.getAllOperators();

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(operators);
        assertThat(response.getBody()).hasSize(2);

        verify(operatorService).getAllOperators();
    }


    // ---------------------------------------------------------
    // Get Operator By ID
    // ---------------------------------------------------------

    @Test
    void shouldGetOperatorById() {

        // Arrange
        Long operatorId = 1L;

        OperatorResponse operatorResponse =
                new OperatorResponse();

        operatorResponse.setId(operatorId);
        operatorResponse.setOperatorName("John Operator");

        when(operatorService.getOperatorById(operatorId))
                .thenReturn(operatorResponse);

        // Act
        ResponseEntity<OperatorResponse> response =
                adminController.getOperatorById(operatorId);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(operatorResponse);

        verify(operatorService).getOperatorById(operatorId);
    }


    // ---------------------------------------------------------
    // Delete Operator
    // ---------------------------------------------------------

    @Test
    void shouldDeleteOperator() {

        // Arrange
        Long operatorId = 1L;

        // Act
        ResponseEntity<Void> response =
                adminController.deleteOperator(operatorId);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(response.getBody()).isNull();

        verify(operatorService).deleteOperator(operatorId);
    }
}