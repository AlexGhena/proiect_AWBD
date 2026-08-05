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
public class UserProfile {

    private UUID id;
    private UUID userId;
    private String firstName;
    private String lastName;
    private String phone;
    private Instant createdAt;
    private Instant updatedAt;
}
