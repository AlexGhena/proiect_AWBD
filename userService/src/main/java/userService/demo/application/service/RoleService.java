package userService.demo.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import userService.demo.domain.exception.DuplicateResourceException;
import userService.demo.domain.exception.ResourceInUseException;
import userService.demo.domain.exception.ResourceNotFoundException;
import userService.demo.domain.model.Role;
import userService.demo.domain.port.in.RoleUseCase;
import userService.demo.domain.port.out.RoleRepositoryPort;
import userService.demo.domain.port.out.UserRepositoryPort;
import userService.demo.domain.port.out.UserRoleRepositoryPort;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
// Every method here is role administration, which the matrix reserves for ADMIN. The one
// exception is listRolesForUser, which a user may call for their own account.
@PreAuthorize("hasRole('ADMIN')")
public class RoleService implements RoleUseCase {

    private final RoleRepositoryPort roleRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final UserRoleRepositoryPort userRoleRepositoryPort;

    @Override
    public Role createRole(Role role) {
        if (roleRepositoryPort.existsByName(role.getName())) {
            throw new DuplicateResourceException("Role " + role.getName() + " already exists");
        }
        role.setId(null);
        return roleRepositoryPort.save(role);
    }

    @Override
    @Transactional(readOnly = true)
    public Role getRole(UUID id) {
        return roleRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Role> listRoles(Pageable pageable) {
        return roleRepositoryPort.findAll(pageable);
    }

    @Override
    public Role updateRole(UUID id, Role updates) {
        Role existing = getRole(id);
        if (updates.getName() != null && !updates.getName().equals(existing.getName())) {
            if (roleRepositoryPort.existsByName(updates.getName())) {
                throw new DuplicateResourceException("Role " + updates.getName() + " already exists");
            }
            existing.setName(updates.getName());
        }
        if (updates.getDescription() != null) {
            existing.setDescription(updates.getDescription());
        }
        return roleRepositoryPort.save(existing);
    }

    @Override
    public void deleteRole(UUID id) {
        if (!roleRepositoryPort.existsById(id)) {
            throw new ResourceNotFoundException("Role " + id + " not found");
        }
        if (roleRepositoryPort.isReferenced(id)) {
            throw new ResourceInUseException("Role " + id + " is still assigned to one or more users");
        }
        roleRepositoryPort.deleteById(id);
    }

    @Override
    public void assignRoleToUser(UUID userId, UUID roleId) {
        if (!userRepositoryPort.existsById(userId)) {
            throw new ResourceNotFoundException("User " + userId + " not found");
        }
        if (!roleRepositoryPort.existsById(roleId)) {
            throw new ResourceNotFoundException("Role " + roleId + " not found");
        }
        if (userRoleRepositoryPort.isAssigned(userId, roleId)) {
            throw new DuplicateResourceException("User " + userId + " already has role " + roleId);
        }
        userRoleRepositoryPort.assign(userId, roleId);
    }

    @Override
    public void unassignRoleFromUser(UUID userId, UUID roleId) {
        if (!userRoleRepositoryPort.isAssigned(userId, roleId)) {
            throw new ResourceNotFoundException("User " + userId + " does not have role " + roleId);
        }
        userRoleRepositoryPort.unassign(userId, roleId);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isSelf(#userId)")
    public List<Role> listRolesForUser(UUID userId) {
        if (!userRepositoryPort.existsById(userId)) {
            throw new ResourceNotFoundException("User " + userId + " not found");
        }
        return userRoleRepositoryPort.findRolesForUser(userId);
    }
}
