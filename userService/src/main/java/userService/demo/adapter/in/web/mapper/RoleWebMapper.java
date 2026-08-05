package userService.demo.adapter.in.web.mapper;

import userService.demo.adapter.in.web.dto.role.CreateRoleRequest;
import userService.demo.adapter.in.web.dto.role.RoleResponse;
import userService.demo.adapter.in.web.dto.role.UpdateRoleRequest;
import userService.demo.domain.model.Role;
import org.springframework.stereotype.Component;

@Component
public class RoleWebMapper {

    public Role toDomain(CreateRoleRequest request) {
        return Role.builder()
                .name(request.name())
                .description(request.description())
                .build();
    }

    public Role toDomain(UpdateRoleRequest request) {
        return Role.builder()
                .name(request.name())
                .description(request.description())
                .build();
    }

    public RoleResponse toResponse(Role domain) {
        return new RoleResponse(domain.getId(), domain.getName(), domain.getDescription());
    }
}
