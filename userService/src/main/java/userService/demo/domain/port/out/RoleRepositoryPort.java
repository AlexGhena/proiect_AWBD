package userService.demo.domain.port.out;

import userService.demo.domain.model.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepositoryPort {

    Role save(Role role);

    Optional<Role> findById(UUID id);

    boolean existsById(UUID id);

    boolean existsByName(String name);

    boolean isReferenced(UUID roleId);

    Page<Role> findAll(Pageable pageable);

    void deleteById(UUID id);
}
