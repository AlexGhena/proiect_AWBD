package userService.demo.adapter.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import userService.demo.adapter.in.web.dto.role.CreateRoleRequest;
import userService.demo.adapter.in.web.dto.role.RoleResponse;
import userService.demo.adapter.in.web.dto.role.UpdateRoleRequest;
import userService.demo.adapter.in.web.mapper.RoleWebMapper;
import userService.demo.domain.model.Role;
import userService.demo.domain.port.in.RoleUseCase;

import java.util.UUID;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleUseCase roleUseCase;
    private final RoleWebMapper mapper;

    @PostMapping
    public ResponseEntity<RoleResponse> create(@Valid @RequestBody CreateRoleRequest request) {
        Role created = roleUseCase.createRole(mapper.toDomain(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(mapper.toResponse(roleUseCase.getRole(id)));
    }

    @GetMapping
    public ResponseEntity<PagedModel<RoleResponse>> list(Pageable pageable) {
        Page<RoleResponse> page = roleUseCase.listRoles(pageable).map(mapper::toResponse);
        return ResponseEntity.ok(new PagedModel<>(page));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateRoleRequest request) {
        Role updated = roleUseCase.updateRole(id, mapper.toDomain(request));
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        roleUseCase.deleteRole(id);
        return ResponseEntity.noContent().build();
    }
}
