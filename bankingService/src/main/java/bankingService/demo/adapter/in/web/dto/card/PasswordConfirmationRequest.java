package bankingService.demo.adapter.in.web.dto.card;

import jakarta.validation.constraints.NotBlank;

public record PasswordConfirmationRequest(
        @NotBlank String password
) {
}
