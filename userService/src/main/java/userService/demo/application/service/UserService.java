package userService.demo.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import userService.demo.security.AuthenticatedUser;
import userService.demo.security.AuthenticatedUserResolver;
import userService.demo.domain.exception.DuplicateResourceException;
import userService.demo.domain.exception.InvalidRegistrationStateException;
import userService.demo.domain.exception.ResourceNotFoundException;
import userService.demo.domain.model.ApprovalStatus;
import userService.demo.domain.model.AppUser;
import userService.demo.domain.model.Role;
import userService.demo.domain.model.UserApprovalResult;
import userService.demo.domain.port.in.UserUseCase;
import userService.demo.domain.port.out.BankingServiceClientPort;
import userService.demo.domain.port.out.PasswordHasherPort;
import userService.demo.domain.port.out.RoleRepositoryPort;
import userService.demo.domain.port.out.UserRepositoryPort;
import userService.demo.domain.port.out.UserRoleRepositoryPort;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserUseCase {

    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final UserRepositoryPort userRepositoryPort;
    private final RoleRepositoryPort roleRepositoryPort;
    private final UserRoleRepositoryPort userRoleRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final BankingServiceClientPort bankingServiceClientPort;

    @Override
    public AppUser register(AppUser user, String rawPassword) {
        // Registration input never decides this: every self-registered account starts disabled,
        // roleless and PENDING until an administrator approves it.
        user.setEnabled(false);
        user.setApprovalStatus(ApprovalStatus.PENDING);
        AppUser created = persistNewUser(user, rawPassword);
        log.info("User registered pending admin approval: id={}", created.getId());
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
        // Only self-registration sets this explicitly (to PENDING); admin-created users skip the
        // approval queue entirely.
        if (user.getApprovalStatus() == null) {
            user.setApprovalStatus(ApprovalStatus.APPROVED);
        }
        AppUser saved = userRepositoryPort.save(user);
        log.info("User created with id={}, username={}", saved.getId(), saved.getUsername());
        return saved;
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
        log.debug("Listing users with pageable={}", pageable);
        return userRepositoryPort.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<AppUser> listDeletedUsers(Pageable pageable) {
        log.debug("Listing soft-deleted users with pageable={}", pageable);
        return userRepositoryPort.findAllDeleted(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<AppUser> listPendingUsers(Pageable pageable) {
        log.debug("Listing pending users with pageable={}", pageable);
        return userRepositoryPort.findAllByApprovalStatus(ApprovalStatus.PENDING, pageable);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public UserApprovalResult approveUser(UUID id) {
        AppUser user = userRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User " + id + " not found"));
        if (user.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new InvalidRegistrationStateException(
                    "User " + id + " is not pending approval (status=" + user.getApprovalStatus() + ")");
        }

        Role defaultRole = roleRepositoryPort.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new IllegalStateException(DEFAULT_ROLE + " is missing from the roles table"));
        userRoleRepositoryPort.assign(user.getId(), defaultRole.getId());

        user.setEnabled(true);
        user.setApprovalStatus(ApprovalStatus.APPROVED);
        AppUser saved = userRepositoryPort.save(user);

        // Called last and inside the same transaction: if bankingService cannot provision the
        // account, the role grant and enable/approve above roll back too, so the admin can retry
        // approve() cleanly instead of leaving a half-approved user with no account.
        String iban = bankingServiceClientPort.provisionAccount(saved.getId());

        log.info("Approved user id={}, assigned role={}, provisioned account iban={}", saved.getId(), DEFAULT_ROLE, iban);
        return UserApprovalResult.builder().user(saved).iban(iban).build();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public AppUser rejectUser(UUID id) {
        AppUser user = userRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User " + id + " not found"));
        if (user.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new InvalidRegistrationStateException(
                    "User " + id + " is not pending approval (status=" + user.getApprovalStatus() + ")");
        }
        user.setApprovalStatus(ApprovalStatus.REJECTED);
        AppUser saved = userRepositoryPort.save(user);
        log.info("Rejected user id={}", saved.getId());
        return saved;
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
        AppUser saved = userRepositoryPort.save(existing);
        log.info("User updated with id={}", saved.getId());
        return saved;
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
        userRepositoryPort.softDelete(id);
        log.info("User soft-deleted with id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("@userSecurity.isSelf(#id)")
    public boolean verifyPassword(UUID id, String rawPassword) {
        AppUser user = userRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User " + id + " not found"));
        return passwordHasherPort.matches(rawPassword, user.getPasswordHash());
    }
}
