package bankingService.demo.adapter.in.web.dto.beneficiary;

public record UpdateBeneficiaryRequest(
        String beneficiaryName,
        String nickname
) {
}
