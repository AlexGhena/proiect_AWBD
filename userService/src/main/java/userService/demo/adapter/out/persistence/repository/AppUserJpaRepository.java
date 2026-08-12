package userService.demo.adapter.out.persistence.repository;

import userService.demo.adapter.out.persistence.entity.AppUserJpaEntity;
import userService.demo.domain.model.ApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppUserJpaRepository extends JpaRepository<AppUserJpaEntity, UUID> {

    Optional<AppUserJpaEntity> findByUsername(String username);

    // Deliberately unfiltered: the username/email unique constraints span every row, deleted or
    // not, so a duplicate check that ignored soft-deleted users could pass here and then fail on
    // the database constraint instead.
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByIdAndDeletedAtIsNull(UUID id);

    Page<AppUserJpaEntity> findAllByDeletedAtIsNull(Pageable pageable);

    Page<AppUserJpaEntity> findAllByDeletedAtIsNotNull(Pageable pageable);

    Page<AppUserJpaEntity> findAllByApprovalStatusAndDeletedAtIsNull(ApprovalStatus approvalStatus, Pageable pageable);
}
