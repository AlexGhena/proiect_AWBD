package userService.demo.application.service;

import userService.demo.domain.exception.DuplicateResourceException;
import userService.demo.domain.exception.ResourceNotFoundException;
import userService.demo.domain.model.AppUser;
import userService.demo.domain.port.in.UserUseCase;
import userService.demo.domain.port.out.PasswordHasherPort;
import userService.demo.domain.port.out.UserRepositoryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class UserService implements UserUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;

    public UserService(UserRepositoryPort userRepositoryPort, PasswordHasherPort passwordHasherPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
    }

    @Override
    public AppUser createUser(AppUser user, String rawPassword) {
        if (userRepositoryPort.existsByUsername(user.getUsername())) {
            throw new DuplicateResourceException("Username " + user.getUsername() + " is already taken");
        }
        if (userRepositoryPort.existsByEmail(user.getEmail())) {
            throw new DuplicateResourceException("Email " + user.getEmail() + " is already registered");
        }
        user.setId(null);
        user.setPasswordHash(passwordHasherPort.hash(rawPassword));
        if (user.getEnabled() == null) {
            user.setEnabled(true);
        }
        return userRepositoryPort.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AppUser getUser(UUID id) {
        return userRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AppUser> listUsers(Pageable pageable) {
        return userRepositoryPort.findAll(pageable);
    }

    @Override
    public AppUser updateUser(UUID id, AppUser updates, String rawPassword) {
        AppUser existing = getUser(id);
        if (updates.getEmail() != null && !updates.getEmail().equals(existing.getEmail())) {
            if (userRepositoryPort.existsByEmail(updates.getEmail())) {
                throw new DuplicateResourceException("Email " + updates.getEmail() + " is already registered");
            }
            existing.setEmail(updates.getEmail());
        }
        if (updates.getEnabled() != null) {
            existing.setEnabled(updates.getEnabled());
        }
        if (rawPassword != null && !rawPassword.isBlank()) {
            existing.setPasswordHash(passwordHasherPort.hash(rawPassword));
        }
        return userRepositoryPort.save(existing);
    }

    @Override
    public void deleteUser(UUID id) {
        if (!userRepositoryPort.existsById(id)) {
            throw new ResourceNotFoundException("User " + id + " not found");
        }
        userRepositoryPort.deleteById(id);
    }
}
