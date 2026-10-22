package com.uvindu.crane_management_system.util;

import com.uvindu.crane_management_system.dto.ChangePasswordRequest;
import com.uvindu.crane_management_system.dto.CreateUserRequest;
import com.uvindu.crane_management_system.dto.UpdateUserRequest;
import com.uvindu.crane_management_system.model.User;
import com.uvindu.crane_management_system.model.UserRole;

public class UserTestDataBuilder {

    public static User createUser(Long id, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setUsername("testuser" + id);
        user.setEmail("test" + id + "@example.com");
        user.setPassword("$2a$10$encodedPasswordHash");
        user.setRole(role);
        user.setActive(true);
        return user;
    }

    public static CreateUserRequest createUserRequest(UserRole role) {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("newuser");
        request.setEmail("newuser@example.com");
        request.setPassword("password123");
        request.setRole(role);
        return request;
    }

    public static UpdateUserRequest createUpdateUserRequest(UserRole role) {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("updateduser");
        request.setEmail("updated@example.com");
        request.setRole(role);
        return request;
    }

    public static ChangePasswordRequest createChangePasswordRequest() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("newPassword123");
        return request;
    }
}
