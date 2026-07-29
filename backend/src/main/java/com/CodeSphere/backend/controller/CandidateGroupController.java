package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.entity.CandidateGroup;
import com.CodeSphere.backend.model.Role;
import com.CodeSphere.backend.repository.CandidateGroupRepository;
import com.CodeSphere.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/candidate-groups")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'EXAMINER')")
public class CandidateGroupController {
    private final CandidateGroupRepository groupRepository;
    private final UserRepository userRepository;

    @GetMapping public ResponseEntity<List<CandidateGroup>> list() { return ResponseEntity.ok(groupRepository.findAll()); }
    @PostMapping public ResponseEntity<CandidateGroup> create(@RequestBody GroupRequest request) {
        List<Long> memberIds = request.memberUserIds() == null ? List.of() : request.memberUserIds().stream().distinct().toList();
        if (request.name() == null || request.name().isBlank() || memberIds.isEmpty()) throw new IllegalArgumentException("A group name and at least one candidate are required.");
        boolean valid = userRepository.findAllById(memberIds).stream().allMatch(user -> user.getRole() == Role.ROLE_CANDIDATE);
        if (!valid || userRepository.findAllById(memberIds).size() != memberIds.size()) throw new IllegalArgumentException("Groups may contain only existing candidate accounts.");
        return ResponseEntity.ok(groupRepository.save(CandidateGroup.builder().name(request.name().trim()).memberUserIds(memberIds).build()));
    }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { groupRepository.deleteById(id); return ResponseEntity.noContent().build(); }
    public record GroupRequest(String name, List<Long> memberUserIds) {}
}
