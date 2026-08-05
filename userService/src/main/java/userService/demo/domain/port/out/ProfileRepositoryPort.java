package userService.demo.domain.port.out;

import userService.demo.domain.model.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ProfileRepositoryPort {

    UserProfile save(UserProfile profile);

    Optional<UserProfile> findById(UUID id);

    Optional<UserProfile> findByUserId(UUID userId);

    boolean existsById(UUID id);

    boolean existsByUserId(UUID userId);

    Page<UserProfile> findAll(Pageable pageable);

    void deleteById(UUID id);
}
