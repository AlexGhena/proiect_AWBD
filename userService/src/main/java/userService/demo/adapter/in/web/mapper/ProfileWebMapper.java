package userService.demo.adapter.in.web.mapper;

import userService.demo.adapter.in.web.dto.profile.CreateProfileRequest;
import userService.demo.adapter.in.web.dto.profile.ProfileResponse;
import userService.demo.adapter.in.web.dto.profile.UpdateProfileRequest;
import userService.demo.domain.model.UserProfile;
import org.springframework.stereotype.Component;

@Component
public class ProfileWebMapper {

    public UserProfile toDomain(CreateProfileRequest request) {
        return UserProfile.builder()
                .userId(request.userId())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .build();
    }

    public UserProfile toDomain(UpdateProfileRequest request) {
        return UserProfile.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .build();
    }

    public ProfileResponse toResponse(UserProfile domain) {
        return new ProfileResponse(
                domain.getId(),
                domain.getUserId(),
                domain.getFirstName(),
                domain.getLastName(),
                domain.getPhone(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}
