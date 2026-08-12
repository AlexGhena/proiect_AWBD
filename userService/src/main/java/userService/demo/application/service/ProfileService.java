package userService.demo.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
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
@Slf4j
public class ProfileService implements ProfileUseCase {

    private final ProfileRepositoryPort profileRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;

    @Override
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.canCreateProfileFor(#profile.userId)")
    public UserProfile createProfile(UserProfile profile) {
        if (!userRepositoryPort.existsById(profile.getUserId())) {
            throw new ResourceNotFoundException("User " + profile.getUserId() + " not found");
        }
        if (profileRepositoryPort.existsByUserId(profile.getUserId())) {
            throw new DuplicateResourceException("User " + profile.getUserId() + " already has a profile");
        }
        profile.setId(null);
        UserProfile saved = profileRepositoryPort.save(profile);
        log.info("Profile created with id={}, userId={}", saved.getId(), saved.getUserId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.ownsProfile(#id)")
    public UserProfile getProfile(UUID id) {
        return profileRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isSelf(#userId)")
    public UserProfile getProfileByUser(UUID userId) {
        return profileRepositoryPort.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile for user " + userId + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserProfile> listProfiles(Pageable pageable) {
        log.debug("Listing profiles with pageable={}", pageable);
        return profileRepositoryPort.findAll(pageable);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.ownsProfile(#id)")
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
        UserProfile saved = profileRepositoryPort.save(existing);
        log.info("Profile updated with id={}", saved.getId());
        return saved;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.ownsProfile(#id)")
    public void deleteProfile(UUID id) {
        if (!profileRepositoryPort.existsById(id)) {
            throw new ResourceNotFoundException("Profile " + id + " not found");
        }
        profileRepositoryPort.deleteById(id);
        log.info("Profile deleted with id={}", id);
    }
}
