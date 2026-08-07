package userService.demo.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import userService.demo.security.AuthenticatedUser;
import userService.demo.security.AuthenticatedUserResolver;
import userService.demo.domain.exception.DuplicateResourceException;
import userService.demo.domain.exception.ResourceNotFoundException;
import userService.demo.domain.model.AppUser;
import userService.demo.domain.model.Role;
import userService.demo.domain.port.in.UserUseCase;
import userService.demo.domain.port.out.PasswordHasherPort;
import userService.demo.domain.port.out.RoleRepositoryPort;
import userService.demo.domain.port.out.UserRepositoryPort;
import userService.demo.domain.port.out.UserRoleRepositoryPort;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService implements UserUseCase {

    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final UserRepositoryPort userRepositoryPort;
    private final RoleRepositoryPort roleRepositoryPort;
    private final UserRoleRepositoryPort userRoleRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @Override
    public AppUser register(AppUser user, String rawPassword) {
        // Force the account active and unprivileged: registration input never decides either.
        user.setEnabled(true);
        AppUser created = persistNewUser(user, rawPassword);

        Role defaultRole = roleRepositoryPort.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new IllegalStateException(DEFAULT_ROLE + " is missing from the roles table"));
        userRoleRepositoryPort.assign(created.getId(), defaultRole.getId());
        return created;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public AppUser createUser(AppUser user, String rawPassword) {
        return persistNewUser(user, rawPassword);
    }

    /**
     * Shared by registration and admin creation. Kept separate so {@link #register} does not have to
     * call the {@code @PreAuthorize}-guarded {@link #createUser} and depend on self-invocation
     * bypassing the security proxy.
     */
    private AppUser persistNewUser(AppUser user, String rawPassword) {
        if (userRepositoryPort.existsByUsername(user.getUsername())) {
            throw new DuplicateResourceException("Username " + user.getUsername() + " is already taken");
        }
        if (userRepositoryPort.existsByEmail(user.getEmail())) {
            throw new DuplicateResourceException("Email " + user.getEmail() + " is already registered");
        }
        user.setId(null);
        user.setPasswordHash(passwordHasherPort.hash(rawPassword));
        if (user.getEnabled() == null) {
            user.setEnabled(true);
        }
        return userRepositoryPort.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isSelf(#id)")
    public AppUser getUser(UUID id) {
        return userRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<AppUser> listUsers(Pageable pageable) {
        return userRepositoryPort.findAll(pageable);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isSelf(#id)")
    public AppUser updateUser(UUID id, AppUser updates, String rawPassword) {
        AppUser existing = userRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User " + id + " not found"));
        if (updates.getEmail() != null && !updates.getEmail().equals(existing.getEmail())) {
            if (userRepositoryPort.existsByEmail(updates.getEmail())) {
                throw new DuplicateResourceException("Email " + updates.getEmail() + " is already registered");
            }
            existing.setEmail(updates.getEmail());
        }
        // Enabling or disabling an account is an administrative action; for a self-update the field
        // is ignored rather than rejected, so a user editing their own profile still succeeds.
        if (updates.getEnabled() != null && isCurrentUserAdmin()) {
            existing.setEnabled(updates.getEnabled());
        }
        if (rawPassword != null && !rawPassword.isBlank()) {
            existing.setPasswordHash(passwordHasherPort.hash(rawPassword));
        }
        return userRepositoryPort.save(existing);
    }

    private boolean isCurrentUserAdmin() {
        return authenticatedUserResolver.current()
                .map(AuthenticatedUser::isAdmin)
                .orElse(false);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(UUID id) {
        if (!userRepositoryPort.existsById(id)) {
            throw new ResourceNotFoundException("User " + id + " not found");
        }
        userRepositoryPort.deleteById(id);
    }
}
