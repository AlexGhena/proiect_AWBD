package userService.demo.domain.port.in;

import userService.demo.domain.model.AppUser;
import userService.demo.domain.model.UserApprovalResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserUseCase {

    AppUser createUser(AppUser user, String rawPassword);

    /**
     * Public self-registration. The account is created disabled and {@code PENDING}, with no role
     * at all - not even ROLE_USER - until an administrator approves it via {@link #approveUser}.
     */
    AppUser register(AppUser user, String rawPassword);

    AppUser getUser(UUID id);

    Page<AppUser> listUsers(Pageable pageable);

    /** Admin-only view of soft-deleted users, otherwise invisible through {@link #getUser} and {@link #listUsers}. */
    Page<AppUser> listDeletedUsers(Pageable pageable);

    /** Admin-only view of accounts awaiting an approve/reject decision. */
    Page<AppUser> listPendingUsers(Pageable pageable);

    /**
     * Admin-only: grants ROLE_USER, enables login, and provisions a bank account (with a freshly
     * generated IBAN) via bankingService. Fails if the user is not currently PENDING.
     */
    UserApprovalResult approveUser(UUID id);

    /** Admin-only: marks a pending registration REJECTED. The account stays disabled and roleless. */
    AppUser rejectUser(UUID id);

    AppUser updateUser(UUID id, AppUser updates, String rawPassword);

    void deleteUser(UUID id);
}
