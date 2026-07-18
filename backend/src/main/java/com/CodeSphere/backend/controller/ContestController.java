package com.CodeSphere.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/contests")
public class ContestController {

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getContests() {
        List<Map<String, Object>> contests = List.of(
                Map.of(
                        "id", 1L,
                        "title", "CodeSphere Monthly Challenge - July 2026",
                        "description", "Competitive programming contest featuring 5 algorithmic problems.",
                        "status", "UPCOMING",
                        "startTime", OffsetDateTime.now().plusDays(3).toString(),
                        "durationMinutes", 180,
                        "participantsCount", 342
                ),
                Map.of(
                        "id", 2L,
                        "title", "Speed Coding Sprint",
                        "description", "Solve simple tasks as fast as you can. Points are time-weighted.",
                        "status", "ACTIVE",
                        "startTime", OffsetDateTime.now().minusMinutes(45).toString(),
                        "durationMinutes", 60,
                        "participantsCount", 1205
                ),
                Map.of(
                        "id", 3L,
                        "title", "System Design Hackathon",
                        "description", "Design a scalable rate-limiting system dashboard.",
                        "status", "COMPLETED",
                        "startTime", OffsetDateTime.now().minusDays(5).toString(),
                        "durationMinutes", 1440,
                        "participantsCount", 89
                )
        );
        return ResponseEntity.ok(contests);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getContestById(@PathVariable Long id) {
        if (id == 1) {
            return ResponseEntity.ok(Map.of(
                    "id", 1L,
                    "title", "CodeSphere Monthly Challenge - July 2026",
                    "description", "Competitive programming contest featuring 5 algorithmic problems.",
                    "status", "UPCOMING",
                    "startTime", OffsetDateTime.now().plusDays(3).toString(),
                    "durationMinutes", 180,
                    "problems", List.of(
                            Map.of("id", 101L, "title", "Two Sum Reloaded", "difficulty", "EASY"),
                            Map.of("id", 102L, "title", "Maximize Container Area", "difficulty", "MEDIUM"),
                            Map.of("id", 103L, "title", "Edit Distance Optimization", "difficulty", "HARD")
                    )
            ));
        } else if (id == 2) {
            return ResponseEntity.ok(Map.of(
                    "id", 2L,
                    "title", "Speed Coding Sprint",
                    "description", "Solve simple tasks as fast as you can. Points are time-weighted.",
                    "status", "ACTIVE",
                    "startTime", OffsetDateTime.now().minusMinutes(45).toString(),
                    "durationMinutes", 60,
                    "problems", List.of(
                            Map.of("id", 201L, "title", "Reverse Vowels", "difficulty", "EASY"),
                            Map.of("id", 202L, "title", "Stack Permutations", "difficulty", "MEDIUM")
                    )
            ));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
