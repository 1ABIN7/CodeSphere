package com.CodeSphere.backend.dto;

import com.CodeSphere.backend.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDto {

    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String bio;
    private String avatarUrl;

    /**
     * Convenience constructor to map from a User entity.
     */
    public ProfileDto(User user) {
        if (user != null) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.email = user.getEmail();
            this.fullName = user.getFullName();
            this.bio = user.getBio();
            this.avatarUrl = user.getAvatarUrl();
        }
    }
}