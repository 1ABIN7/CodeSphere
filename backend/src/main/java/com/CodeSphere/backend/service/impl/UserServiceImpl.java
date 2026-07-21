package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.dto.ProfileDto;
import com.CodeSphere.backend.dto.UserDashboardDto;
import com.CodeSphere.backend.model.User;
import com.CodeSphere.backend.repository.UserRepository;
import com.CodeSphere.backend.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public ProfileDto getProfileByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));
        return new ProfileDto(user);
    }

    @Override
    public ProfileDto updateMyProfile(String currentUsername, ProfileDto updates) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user context missing"));

        if (updates.getFullName() != null) user.setFullName(updates.getFullName());
        if (updates.getBio() != null) user.setBio(updates.getBio());
        if (updates.getAvatarUrl() != null) user.setAvatarUrl(updates.getAvatarUrl());

        User savedUser = userRepository.save(user);
        return new ProfileDto(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDashboardDto getDashboardData(String username) {
        ProfileDto profile = getProfileByUsername(username);

        // Mocking stats for now. Switch these targets to your submission/analytics repos when ready.
        long totalSubmissions = 42;
        long solvedQuestionsCount = 17;
        Map<String, Long> difficultyStats = Map.of("Easy", 10L, "Medium", 6L, "Hard", 1L);

        return new UserDashboardDto(profile, totalSubmissions, solvedQuestionsCount, difficultyStats);
    }

    @Override
    public Long getUserIdByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with username: " + username));
    }

    @Override
    public void updateUserRole(Long userId, String role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        user.setRole(com.CodeSphere.backend.model.Role.valueOf(role));
        userRepository.save(user);
    }
}