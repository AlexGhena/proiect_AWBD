package userService.demo.domain.port.in;

import userService.demo.domain.model.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface RoleUseCase {

    Role createRole(Role role);

    Role getRole(UUID id);

    Page<Role> listRoles(Pageable pageable);

    Role updateRole(UUID id, Role updates);

    void deleteRole(UUID id);

    void assignRoleToUser(UUID userId, UUID roleId);

    void unassignRoleFromUser(UUID userId, UUID roleId);

    List<Role> listRolesForUser(UUID userId);
}
