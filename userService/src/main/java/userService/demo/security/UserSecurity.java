package userService.demo.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import userService.demo.domain.port.out.AddressRepositoryPort;
import userService.demo.domain.port.out.ProfileRepositoryPort;

import java.util.UUID;

/**
 * Resource-ownership checks referenced from {@code @PreAuthorize} as {@code @userSecurity}.
 *
 * <p>URL role rules alone are not enough: a ROLE_USER passing any of these endpoints must also own
 * the record being touched, which is decided here against the UUID carried in the token's
 * {@code sub} claim.
 */
@Component("userSecurity")
@RequiredArgsConstructor
public class UserSecurity {

    private final AuthenticatedUserResolver resolver;
    private final ProfileRepositoryPort profileRepositoryPort;
    private final AddressRepositoryPort addressRepositoryPort;

    /** True when the caller is acting on their own user record. */
    public boolean isSelf(UUID userId) {
        return resolver.current()
                .map(user -> user.id().equals(userId))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean ownsProfile(UUID profileId) {
        return resolver.current()
                .flatMap(user -> profileRepositoryPort.findById(profileId)
                        .map(profile -> profile.getUserId().equals(user.id())))
                .orElse(false);
    }

    /** A caller may create a profile only for themselves. */
    public boolean canCreateProfileFor(UUID userId) {
        return isSelf(userId);
    }

    @Transactional(readOnly = true)
    public boolean ownsAddress(UUID addressId) {
        return addressRepositoryPort.findById(addressId)
                .map(address -> ownsProfile(address.getProfileId()))
                .orElse(false);
    }
}
