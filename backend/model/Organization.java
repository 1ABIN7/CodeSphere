package com.CodeSphere.backend.service;

import com.CodeSphere.backend.dto.OrganizationRequest;
import com.CodeSphere.backend.model.Organization;
import com.CodeSphere.backend.model.User;
import com.CodeSphere.backend.repository.OrganizationRepository;
import com.CodeSphere.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    // --- CRUD Operations ---

    @Transactional
    public Organization createOrganization(OrganizationRequest request) {
        if (organizationRepository.existsByName(request.getName())) {
            throw new RuntimeException("Error: Organization name is already taken!");
        }

        Organization org = Organization.builder()
                .name(request.getName())
                .build(); // @PrePersist sets createdAt automatically

        return organizationRepository.save(org);
    }

    public List<Organization> getAllOrganizations() {
        return organizationRepository.findAll();
    }

    public Organization getOrganizationById(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: Organization not found with ID: " + id));
    }

    @Transactional
    public Organization updateOrganization(Long id, OrganizationRequest request) {
        Organization org = getOrganizationById(id);
        org.setName(request.getName());
        return organizationRepository.save(org);
    }

    @Transactional
    public void deleteOrganization(Long id) {
        Organization org = getOrganizationById(id);
        // Safely detach users so we don't drop accounts during deletion
        if (org.getUsers() != null) {
            for (User user : org.getUsers()) {
                user.setOrganization(null);
                userRepository.save(user);
            }
        }
        organizationRepository.delete(org);
    }

    // --- Member Management ---

    @Transactional
    public void addMember(Long organizationId, Long userId) {
        Organization org = getOrganizationById(organizationId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Error: User not found with ID: " + userId));

        user.setOrganization(org);
        userRepository.save(user);
    }

    @Transactional
    public void removeMember(Long organizationId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Error: User not found with ID: " + userId));

        if (user.getOrganization() == null || !user.getOrganization().getId().equals(organizationId)) {
            throw new RuntimeException("Error: User is not a member of this organization.");
        }

        user.setOrganization(null);
        userRepository.save(user);
    }

    public Set<User> getMembers(Long organizationId) {
        return getOrganizationById(organizationId).getUsers();
    }
}