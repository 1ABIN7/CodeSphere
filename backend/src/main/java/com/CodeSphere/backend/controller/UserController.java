package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.dto.ProfileDto;
import com.CodeSphere.backend.dto.UserDashboardDto;
import com.CodeSphere.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
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