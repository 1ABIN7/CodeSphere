package com.CodeSphere.backend.service;

import com.CodeSphere.backend.dto.ProfileDto;
import com.CodeSphere.backend.dto.UserDashboardDto;

public interface UserService {

    ProfileDto getProfileByUsername(String username);

    ProfileDto updateMyProfile(String currentUsername, ProfileDto updates);

    UserDashboardDto getDashboardData(String username);

    // --- Added methods for AdminController and AuthController ---
    void updateUserRole(Long userId, String newRole);

    Long getUserIdByUsername(String username);
}