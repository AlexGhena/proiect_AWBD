package userService.demo.adapter.out.client.dto;

import java.util.UUID;

/** Body sent to bankingService's {@code POST /internal/accounts/provision}. */
public record ProvisionAccountRequest(UUID userId, String currency) {
}
