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
import org.springframework.web.bind.annotation.RestController;
import userService.demo.adapter.in.web.dto.profile.CreateProfileRequest;
import userService.demo.adapter.in.web.dto.profile.ProfileResponse;
import userService.demo.adapter.in.web.dto.profile.UpdateProfileRequest;
import userService.demo.adapter.in.web.mapper.ProfileWebMapper;
import userService.demo.domain.model.UserProfile;
import userService.demo.domain.port.in.ProfileUseCase;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileUseCase profileUseCase;
    private final ProfileWebMapper mapper;

    @PostMapping("/api/profiles")
    public ResponseEntity<ProfileResponse> create(@Valid @RequestBody CreateProfileRequest request) {
        UserProfile created = profileUseCase.createProfile(mapper.toDomain(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(created));
    }

    @GetMapping("/api/profiles/{id}")
    public ResponseEntity<ProfileResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(mapper.toResponse(profileUseCase.getProfile(id)));
    }

    @GetMapping("/api/profiles")
    public ResponseEntity<PagedModel<ProfileResponse>> list(Pageable pageable) {
        Page<ProfileResponse> page = profileUseCase.listProfiles(pageable).map(mapper::toResponse);
        return ResponseEntity.ok(new PagedModel<>(page));
    }

    @GetMapping("/api/users/{userId}/profile")
    public ResponseEntity<ProfileResponse> getByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(mapper.toResponse(profileUseCase.getProfileByUser(userId)));
    }

    @PutMapping("/api/profiles/{id}")
    public ResponseEntity<ProfileResponse> update(@PathVariable UUID id,
                                                    @Valid @RequestBody UpdateProfileRequest request) {
        UserProfile updated = profileUseCase.updateProfile(id, mapper.toDomain(request));
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @DeleteMapping("/api/profiles/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        profileUseCase.deleteProfile(id);
        return ResponseEntity.noContent().build();
    }
}
