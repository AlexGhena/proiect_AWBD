package bankingService.demo.adapter.in.web.dto.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CompensateAccountRequest(
        @NotBlank String originalIdempotencyKey,
        @NotNull UUID sagaId
) {
}
