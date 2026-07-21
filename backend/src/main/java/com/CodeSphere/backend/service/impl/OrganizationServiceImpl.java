package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.dto.OrganizationRequest;
import com.CodeSphere.backend.model.Organization;
import com.CodeSphere.backend.model.User;
import com.CodeSphere.backend.repository.OrganizationRepository;
import com.CodeSphere.backend.repository.UserRepository;
import com.CodeSphere.backend.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    // --- CRUD Operations ---

    @Override
    @Transactional
    public Organization createOrganization(OrganizationRequest request) {
        if (organizationRepository.existsByName(request.getName())) {
            throw new RuntimeException("Error: Organization name is already taken!");
        }

        Organization org = Organization.builder()
                .name(request.getName())
                .build();

        return organizationRepository.save(org);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Organization> getAllOrganizations() {
        return organizationRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Organization getOrganizationById(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: Organization not found with ID: " + id));
    }

    @Override
    @Transactional
    public Organization updateOrganization(Long id, OrganizationRequest request) {
        Organization org = getOrganizationById(id);
        org.setName(request.getName());
        return organizationRepository.save(org);
    }

    @Override
    @Transactional
    public void deleteOrganization(Long id) {
        Organization org = getOrganizationById(id);
        // Safely detach users so accounts aren't dropped during deletion
        if (org.getUsers() != null) {
            for (User user : org.getUsers()) {
                user.setOrganization(null);
                userRepository.save(user);
            }
        }
        organizationRepository.delete(org);
    }

    // --- Member Management ---

    @Override
    @Transactional
    public void addMember(Long organizationId, Long userId) {
        Organization org = getOrganizationById(organizationId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Error: User not found with ID: " + userId));

        user.setOrganization(org);
        userRepository.save(user);
    }

    @Override
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

    @Override
    @Transactional(readOnly = true)
    public Set<User> getMembers(Long organizationId) {
        return getOrganizationById(organizationId).getUsers();
    }
}