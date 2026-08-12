package userService.demo.adapter.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import userService.demo.adapter.in.web.dto.common.PageResponse;
import userService.demo.adapter.in.web.dto.role.RoleResponse;
import userService.demo.adapter.in.web.dto.user.CreateUserRequest;
import userService.demo.adapter.in.web.dto.user.UpdateUserRequest;
import userService.demo.adapter.in.web.dto.user.UserApprovalResponse;
import userService.demo.adapter.in.web.dto.user.UserResponse;
import userService.demo.adapter.in.web.mapper.RoleWebMapper;
import userService.demo.adapter.in.web.mapper.UserWebMapper;
import userService.demo.adapter.in.web.support.PaginationParamsResolver;
import userService.demo.domain.model.AppUser;
import userService.demo.domain.model.UserApprovalResult;
import userService.demo.domain.port.in.RoleUseCase;
import userService.demo.domain.port.in.UserUseCase;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private static final Set<String> SORT_FIELDS = Set.of("username", "email", "createdAt");
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    private final UserUseCase userUseCase;
    private final RoleUseCase roleUseCase;
    private final UserWebMapper userMapper;
    private final RoleWebMapper roleMapper;
    private final PaginationParamsResolver pageableResolver;

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
    public ResponseEntity<PageResponse<UserResponse>> list(@RequestParam(required = false) Integer page,
                                                             @RequestParam(required = false) Integer size,
                                                             @RequestParam(required = false) String sortBy,
                                                             @RequestParam(required = false) String sortDirection) {
        Pageable pageable = pageableResolver.resolve(page, size, sortBy, sortDirection, SORT_FIELDS, DEFAULT_SORT_FIELD);
        Page<UserResponse> result = userUseCase.listUsers(pageable).map(userMapper::toResponse);
        return ResponseEntity.ok(PageResponse.of(result));
    }

    /** Admin-only: users hidden from every other endpoint because they were soft-deleted. */
    @GetMapping("/deleted")
    public ResponseEntity<PageResponse<UserResponse>> listDeleted(@RequestParam(required = false) Integer page,
                                                                    @RequestParam(required = false) Integer size,
                                                                    @RequestParam(required = false) String sortBy,
                                                                    @RequestParam(required = false) String sortDirection) {
        Pageable pageable = pageableResolver.resolve(page, size, sortBy, sortDirection, SORT_FIELDS, DEFAULT_SORT_FIELD);
        Page<UserResponse> result = userUseCase.listDeletedUsers(pageable).map(userMapper::toResponse);
        return ResponseEntity.ok(PageResponse.of(result));
    }

    /** Admin-only: accounts awaiting an approve/reject decision. */
    @GetMapping("/pending")
    public ResponseEntity<PageResponse<UserResponse>> listPending(@RequestParam(required = false) Integer page,
                                                                     @RequestParam(required = false) Integer size,
                                                                     @RequestParam(required = false) String sortBy,
                                                                     @RequestParam(required = false) String sortDirection) {
        Pageable pageable = pageableResolver.resolve(page, size, sortBy, sortDirection, SORT_FIELDS, DEFAULT_SORT_FIELD);
        Page<UserResponse> result = userUseCase.listPendingUsers(pageable).map(userMapper::toResponse);
        return ResponseEntity.ok(PageResponse.of(result));
    }

    /** Admin-only: grants ROLE_USER, enables login, and provisions a bank account with a fresh IBAN. */
    @PostMapping("/{id}/approve")
    public ResponseEntity<UserApprovalResponse> approve(@PathVariable UUID id) {
        UserApprovalResult result = userUseCase.approveUser(id);
        return ResponseEntity.ok(new UserApprovalResponse(userMapper.toResponse(result.user()), result.iban()));
    }

    /** Admin-only: rejects a pending registration. The account stays disabled and roleless. */
    @PostMapping("/{id}/reject")
    public ResponseEntity<UserResponse> reject(@PathVariable UUID id) {
        return ResponseEntity.ok(userMapper.toResponse(userUseCase.rejectUser(id)));
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
