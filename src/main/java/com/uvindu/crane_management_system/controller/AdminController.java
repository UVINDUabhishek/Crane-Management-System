package com.uvindu.crane_management_system.controller;

import com.uvindu.crane_management_system.dto.*;
import com.uvindu.crane_management_system.model.User;
import com.uvindu.crane_management_system.service.OperatorService;
import com.uvindu.crane_management_system.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final OperatorService operatorService;

    // Change Admin password
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequest request, Authentication authentication) {

        userService.changeAdminPassword(authentication.getName(), request);

        return ResponseEntity.noContent().build();

    }

    // Create a User
    @PostMapping("/users")
    public ResponseEntity<User> createUser(@RequestBody CreateUserRequest request) {

        return ResponseEntity.ok(userService.register(request));

    }

    // Get all users
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {

        return ResponseEntity.ok(userService.getAllUsers());

    }

    // Get user by ID
    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {

        return ResponseEntity.ok(userService.getUserById(id));

    }

    // Update User
    @PutMapping("/users/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {

        return ResponseEntity.ok(userService.updateUser(id, request));

    }

    // Delete User
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable long id) {

        userService.deleteUser(id);

        return ResponseEntity.noContent().build();
    }

    // Create Operator
    @PostMapping("/operators")
    public ResponseEntity<OperatorResponse> createOperator(@Valid @RequestBody CreateOperatorRequest request) {

        return ResponseEntity.ok(operatorService.createOperator(request));
    }

    // Get all Operators
    @GetMapping("/operators")
    public ResponseEntity<List<OperatorResponse>> getAllOperators() {

        return ResponseEntity.ok(operatorService.getAllOperators());
    }

    // Get Operator by ID
    @GetMapping("/operators/{id}")
    public ResponseEntity<OperatorResponse> getOperatorById(@PathVariable Long id) {

        return ResponseEntity.ok(operatorService.getOperatorById(id));
    }

    // Delete Operator
    @DeleteMapping("/operators/{id}")
    public ResponseEntity<Void> deleteOperator(@PathVariable Long id) {

        operatorService.deleteOperator(id);

        return ResponseEntity.noContent().build();
    }

}
