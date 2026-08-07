package userService.demo.adapter.in.web.mapper;

import userService.demo.adapter.in.web.dto.user.CreateUserRequest;
import userService.demo.adapter.in.web.dto.user.UpdateUserRequest;
import userService.demo.adapter.in.web.dto.user.UserResponse;
import userService.demo.domain.model.AppUser;
import org.springframework.stereotype.Component;

@Component
public class UserWebMapper {

    public AppUser toDomain(CreateUserRequest request) {
        return AppUser.builder()
                .username(request.username())
                .email(request.email())
                .enabled(request.enabled())
                .build();
    }

    public AppUser toDomain(UpdateUserRequest request) {
        return AppUser.builder()
                .email(request.email())
                .enabled(request.enabled())
                .build();
    }

    public UserResponse toResponse(AppUser domain) {
        return new UserResponse(
                domain.getId(),
                domain.getUsername(),
                domain.getEmail(),
                domain.getEnabled(),
                domain.getCreatedAt(),
                domain.getUpdatedAt(),
                domain.getDeletedAt()
        );
    }
}
