package userService.demo.adapter.out.persistence.repository;

import userService.demo.adapter.out.persistence.entity.AddressJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AddressJpaRepository extends JpaRepository<AddressJpaEntity, UUID> {

    List<AddressJpaEntity> findByProfile_Id(UUID profileId);
}
