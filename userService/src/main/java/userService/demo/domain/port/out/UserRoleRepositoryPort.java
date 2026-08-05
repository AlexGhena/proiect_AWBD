package userService.demo.domain.port.out;

import userService.demo.domain.model.Role;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepositoryPort {

    void assign(UUID userId, UUID roleId);

    void unassign(UUID userId, UUID roleId);

    boolean isAssigned(UUID userId, UUID roleId);

    List<Role> findRolesForUser(UUID userId);
}
