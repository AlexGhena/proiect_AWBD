package userService.demo.domain.port.in;

import userService.demo.domain.model.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserUseCase {

    AppUser createUser(AppUser user, String rawPassword);

    /**
     * Public self-registration. Always grants ROLE_USER and nothing else, regardless of what the
     * caller sent; escalating to ROLE_ADMIN is an administrator-only action.
     */
    AppUser register(AppUser user, String rawPassword);

    AppUser getUser(UUID id);

    Page<AppUser> listUsers(Pageable pageable);

    AppUser updateUser(UUID id, AppUser updates, String rawPassword);

    void deleteUser(UUID id);
}
