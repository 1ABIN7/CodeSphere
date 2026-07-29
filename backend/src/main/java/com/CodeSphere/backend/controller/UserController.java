package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.dto.ProfileDto;
import com.CodeSphere.backend.dto.UserDashboardDto;
import com.CodeSphere.backend.service.UserService;
import com.CodeSphere.backend.repository.UserRepository;
import com.CodeSphere.backend.model.Role;
import com.CodeSphere.backend.dto.CandidateOptionDto;
import com.CodeSphere.backend.dto.AdminUserDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @GetMapping("/candidates")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'EXAMINER')")
    public ResponseEntity<List<CandidateOptionDto>> getCandidates() {
        return ResponseEntity.ok(userRepository.findByRole(Role.ROLE_CANDIDATE).stream()
                .map(user -> new CandidateOptionDto(user.getId(), user.getUsername(), user.getFullName(), user.getEmail()))
                .toList());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'EXAMINER')")
    public ResponseEntity<List<AdminUserDto>> getUsers() {
        return ResponseEntity.ok(userRepository.findAll().stream()
                .map(AdminUserDto::from)
                .toList());
    }

    // GET /api/v1/users/{username}/profile
    @GetMapping("/{username}/profile")
    public ResponseEntity<ProfileDto> getUserProfile(@PathVariable("username") String username) {
        return ResponseEntity.ok(userService.getProfileByUsername(username));
    }

    // PUT /api/v1/users/me/profile
    @PutMapping("/me/profile")
    public ResponseEntity<ProfileDto> updateMyProfile(Principal principal, @RequestBody ProfileDto profileDto) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(userService.updateMyProfile(principal.getName(), profileDto));
    }

    // GET /api/v1/users/me/dashboard
    @GetMapping("/me/dashboard")
    public ResponseEntity<UserDashboardDto> getMyDashboard(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(userService.getDashboardData(principal.getName()));
    }
}
