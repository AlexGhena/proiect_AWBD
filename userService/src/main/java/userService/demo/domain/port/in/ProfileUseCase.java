package userService.demo.domain.port.in;

import userService.demo.domain.model.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProfileUseCase {

    UserProfile createProfile(UserProfile profile);

    UserProfile getProfile(UUID id);

    UserProfile getProfileByUser(UUID userId);

    Page<UserProfile> listProfiles(Pageable pageable);

    UserProfile updateProfile(UUID id, UserProfile updates);

    void deleteProfile(UUID id);
}
