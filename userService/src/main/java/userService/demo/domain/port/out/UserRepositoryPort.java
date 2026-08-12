package userService.demo.domain.port.out;

import userService.demo.domain.model.ApprovalStatus;
import userService.demo.domain.model.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {

    AppUser save(AppUser user);

    /** Active users only; a soft-deleted user is treated as not found. */
    Optional<AppUser> findById(UUID id);

    /** Active users only; a soft-deleted user is treated as not found. */
    Optional<AppUser> findByUsername(String username);

    /** True only for an active user; a soft-deleted user does not "exist" for business rules. */
    boolean existsById(UUID id);

    // Checked against every row including soft-deleted ones - see the repository for why.
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /** Active users only. */
    Page<AppUser> findAll(Pageable pageable);

    /** Soft-deleted users only, for admin visibility into otherwise-hidden accounts. */
    Page<AppUser> findAllDeleted(Pageable pageable);

    /** Active, non-deleted users awaiting an admin decision. */
    Page<AppUser> findAllByApprovalStatus(ApprovalStatus approvalStatus, Pageable pageable);

    /** Stamps deleted_at and disables login; the row and its history are kept. */
    void softDelete(UUID id);
}
