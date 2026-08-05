package userService.demo.domain.port.in;

import userService.demo.domain.model.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserUseCase {

    AppUser createUser(AppUser user, String rawPassword);

    AppUser getUser(UUID id);

    Page<AppUser> listUsers(Pageable pageable);

    AppUser updateUser(UUID id, AppUser updates, String rawPassword);

    void deleteUser(UUID id);
}
