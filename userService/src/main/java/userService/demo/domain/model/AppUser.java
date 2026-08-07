package userService.demo.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppUser {

    private UUID id;
    private String username;
    private String email;
    private String passwordHash;
    private Boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}
