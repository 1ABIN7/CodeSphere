package com.CodeSphere.backend.service;

import com.CodeSphere.backend.dto.OrganizationRequest;
import com.CodeSphere.backend.model.Organization;
import com.CodeSphere.backend.model.User;

import java.util.List;
import java.util.Set;

public interface OrganizationService {

    Organization createOrganization(OrganizationRequest request);

    List<Organization> getAllOrganizations();

    Organization getOrganizationById(Long id);

    Organization updateOrganization(Long id, OrganizationRequest request);

    void deleteOrganization(Long id);

    void addMember(Long organizationId, Long userId);

    void removeMember(Long organizationId, Long userId);

    Set<User> getMembers(Long organizationId);
}