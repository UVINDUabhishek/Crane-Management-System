package com.uvindu.crane_management_system.service;

import com.uvindu.crane_management_system.dto.*;
import com.uvindu.crane_management_system.model.User;
import com.uvindu.crane_management_system.model.UserRole;
import com.uvindu.crane_management_system.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Register a User
    public UserResponse register(CreateUserRequest request) {

        User user = new User();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());

        // encrypt password
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        user.setRole(request.getRole());

        return mapToResponse(userRepository.save(user));
    }

    // Change Admin password
    public void changeAdminPassword(String email, ChangePasswordRequest request) {

        User admin = userRepository.findByEmail(email)
                .orElseThrow(()-> new RuntimeException("User not found"));


        if (admin.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("Only admin can change the password");
        }

        if(!passwordEncoder.matches(
                request.getCurrentPassword(),
                admin.getPassword())) {

            throw new RuntimeException("Current password is incorrect");
        }

        admin.setPassword(
                passwordEncoder.encode(request.getNewPassword())
        );

        userRepository.save(admin);

    }

    // Get all Users
    public List<UserResponse> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found"));
        return mapToResponse(user);
    }

    // Get user by ID
    public UserResponse getUserById(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(()->
                        new RuntimeException("User not found"));

        return mapToResponse(user);
    }

    // Update User
    public UserResponse updateUser(Long id, UpdateUserRequest request) {

        User user = userRepository.findById(id)
                .orElseThrow(()->
                        new RuntimeException("User not found"));

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());

        User updateUser = userRepository.save(user);

        return mapToResponse(updateUser);
    }

    // Delete User
    public void deleteUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(()->
                        new RuntimeException("User not found"));

        if (user.getRole() == UserRole.ADMIN) {
            throw new RuntimeException("Admin account cannot be deactivated");
        }

        user.setActive(false);

        userRepository.save(user);
    }

    // Mapping Method
    private UserResponse mapToResponse(User user) {

        UserResponse response = new UserResponse();
        response.setId(user.getId());

        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());

        return response;
    }

}
