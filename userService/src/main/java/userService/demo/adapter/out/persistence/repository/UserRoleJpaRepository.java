package userService.demo.adapter.out.persistence.repository;

import userService.demo.adapter.out.persistence.entity.UserRoleId;
import userService.demo.adapter.out.persistence.entity.UserRoleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserRoleJpaRepository extends JpaRepository<UserRoleJpaEntity, UserRoleId> {

    boolean existsByRole_Id(UUID roleId);

    List<UserRoleJpaEntity> findById_UserId(UUID userId);
}
