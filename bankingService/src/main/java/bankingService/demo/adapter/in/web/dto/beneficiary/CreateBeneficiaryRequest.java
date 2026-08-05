package bankingService.demo.adapter.in.web.dto.beneficiary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CreateBeneficiaryRequest(
        @NotNull UUID ownerAccountId,
        @NotBlank String beneficiaryName,
        @NotBlank @Pattern(regexp = "^[A-Z0-9]{15,34}$", message = "beneficiaryIban must be 15-34 uppercase alphanumeric characters")
        String beneficiaryIban,
        String nickname
) {
}
