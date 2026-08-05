package userService.demo.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import userService.demo.domain.exception.DuplicateResourceException;
import userService.demo.domain.exception.ResourceNotFoundException;
import userService.demo.domain.model.UserProfile;
import userService.demo.domain.port.in.ProfileUseCase;
import userService.demo.domain.port.out.ProfileRepositoryPort;
import userService.demo.domain.port.out.UserRepositoryPort;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ProfileService implements ProfileUseCase {

    private final ProfileRepositoryPort profileRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;

    @Override
    public UserProfile createProfile(UserProfile profile) {
        if (!userRepositoryPort.existsById(profile.getUserId())) {
            throw new ResourceNotFoundException("User " + profile.getUserId() + " not found");
        }
        if (profileRepositoryPort.existsByUserId(profile.getUserId())) {
            throw new DuplicateResourceException("User " + profile.getUserId() + " already has a profile");
        }
        profile.setId(null);
        return profileRepositoryPort.save(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfile getProfile(UUID id) {
        return profileRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfile getProfileByUser(UUID userId) {
        return profileRepositoryPort.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile for user " + userId + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserProfile> listProfiles(Pageable pageable) {
        return profileRepositoryPort.findAll(pageable);
    }

    @Override
    public UserProfile updateProfile(UUID id, UserProfile updates) {
        UserProfile existing = getProfile(id);
        if (updates.getFirstName() != null) {
            existing.setFirstName(updates.getFirstName());
        }
        if (updates.getLastName() != null) {
            existing.setLastName(updates.getLastName());
        }
        if (updates.getPhone() != null) {
            existing.setPhone(updates.getPhone());
        }
        return profileRepositoryPort.save(existing);
    }

    @Override
    public void deleteProfile(UUID id) {
        if (!profileRepositoryPort.existsById(id)) {
            throw new ResourceNotFoundException("Profile " + id + " not found");
        }
        profileRepositoryPort.deleteById(id);
    }
}
