package userService.demo.domain.port.out;

import userService.demo.domain.model.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {

    AppUser save(AppUser user);

    Optional<AppUser> findById(UUID id);

    Optional<AppUser> findByUsername(String username);

    boolean existsById(UUID id);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Page<AppUser> findAll(Pageable pageable);

    void deleteById(UUID id);
}
