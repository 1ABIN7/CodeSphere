package com.CodeSphere.backend.dto;

import com.CodeSphere.backend.model.User;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDto {
    private String fullName;
    private String bio;
    private String avatarUrl;

    // Add this explicit mapping constructor
    public ProfileDto(User user) {
        this.fullName = user.getUsername(); // or user.getFullName() if it exists
        this.bio = "";
        this.avatarUrl = "";
    }
}