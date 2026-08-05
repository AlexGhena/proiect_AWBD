package userService.demo.adapter.in.web;

import userService.demo.adapter.in.web.dto.role.RoleResponse;
import userService.demo.adapter.in.web.dto.user.CreateUserRequest;
import userService.demo.adapter.in.web.dto.user.UpdateUserRequest;
import userService.demo.adapter.in.web.dto.user.UserResponse;
import userService.demo.adapter.in.web.mapper.RoleWebMapper;
import userService.demo.adapter.in.web.mapper.UserWebMapper;
import userService.demo.domain.model.AppUser;
import userService.demo.domain.port.in.RoleUseCase;
import userService.demo.domain.port.in.UserUseCase;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserUseCase userUseCase;
    private final RoleUseCase roleUseCase;
    private final UserWebMapper userMapper;
    private final RoleWebMapper roleMapper;

    public UserController(UserUseCase userUseCase, RoleUseCase roleUseCase,
                           UserWebMapper userMapper, RoleWebMapper roleMapper) {
        this.userUseCase = userUseCase;
        this.roleUseCase = roleUseCase;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        AppUser created = userUseCase.createUser(userMapper.toDomain(request), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toResponse(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(userMapper.toResponse(userUseCase.getUser(id)));
    }

    @GetMapping
    public ResponseEntity<PagedModel<UserResponse>> list(Pageable pageable) {
        Page<UserResponse> page = userUseCase.listUsers(pageable).map(userMapper::toResponse);
        return ResponseEntity.ok(new PagedModel<>(page));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        AppUser updated = userUseCase.updateUser(id, userMapper.toDomain(request), request.password());
        return ResponseEntity.ok(userMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userUseCase.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/roles")
    public ResponseEntity<List<RoleResponse>> listRoles(@PathVariable UUID userId) {
        List<RoleResponse> roles = roleUseCase.listRolesForUser(userId).stream()
                .map(roleMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(roles);
    }

    @PostMapping("/{userId}/roles/{roleId}")
    public ResponseEntity<Void> assignRole(@PathVariable UUID userId, @PathVariable UUID roleId) {
        roleUseCase.assignRoleToUser(userId, roleId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{userId}/roles/{roleId}")
    public ResponseEntity<Void> unassignRole(@PathVariable UUID userId, @PathVariable UUID roleId) {
        roleUseCase.unassignRoleFromUser(userId, roleId);
        return ResponseEntity.noContent().build();
    }
}
