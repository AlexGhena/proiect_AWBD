package userService.demo.adapter.out.persistence.repository;

import userService.demo.adapter.out.persistence.entity.UserProfileJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileJpaRepository extends JpaRepository<UserProfileJpaEntity, UUID> {

    Optional<UserProfileJpaEntity> findByUser_Id(UUID userId);

    boolean existsByUser_Id(UUID userId);
}
