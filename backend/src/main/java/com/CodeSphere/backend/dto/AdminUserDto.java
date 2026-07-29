package com.CodeSphere.backend.dto;

import com.CodeSphere.backend.model.User;
import lombok.Value;

import java.time.LocalDateTime;

/** Safe user data for organization administration; never exposes credentials. */
@Value
public class AdminUserDto {
    Long id;
    String username;
    String fullName;
    String email;
    String role;
    LocalDateTime createdAt;

    public static AdminUserDto from(User user) {
        return new AdminUserDto(user.getId(), user.getUsername(), user.getFullName(), user.getEmail(),
                user.getRole().name(), user.getCreatedAt());
    }
}
