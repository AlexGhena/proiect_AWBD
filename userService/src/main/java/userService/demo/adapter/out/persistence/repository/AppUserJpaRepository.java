package userService.demo.adapter.out.persistence.repository;

import userService.demo.adapter.out.persistence.entity.AppUserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AppUserJpaRepository extends JpaRepository<AppUserJpaEntity, UUID> {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
