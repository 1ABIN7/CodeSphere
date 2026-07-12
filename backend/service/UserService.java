package com.codesphere.backend.service;

import com.codesphere.backend.model.User;
import com.codesphere.backend.model.ProfileDto;
import com.codesphere.backend.model.UserDashboardDto;
import com.codesphere.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ProfileDto getProfileByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));
        return new ProfileDto(user);
    }

    public ProfileDto updateMyProfile(String currentUsername, ProfileDto updates) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user context missing"));

        if (updates.getFullName() != null) user.setFullName(updates.getFullName());
        if (updates.getBio() != null) user.setBio(updates.getBio());
        if (updates.getAvatarUrl() != null) user.setAvatarUrl(updates.getAvatarUrl());

        User savedUser = userRepository.save(user);
        return new ProfileDto(savedUser);
    }

    @Transactional(readOnly = true)
    public UserDashboardDto getDashboardData(String username) {
        ProfileDto profile = getProfileByUsername(username);

        // Mocking stats for now. Switch these targets to your submission/analytics repos when ready.
        long totalSubmissions = 42;
        long solvedQuestionsCount = 17;
        Map<String, Long> difficultyStats = Map.of("Easy", 10L, "Medium", 6L, "Hard", 1L);

        return new UserDashboardDto(profile, totalSubmissions, solvedQuestionsCount, difficultyStats);
    }
}