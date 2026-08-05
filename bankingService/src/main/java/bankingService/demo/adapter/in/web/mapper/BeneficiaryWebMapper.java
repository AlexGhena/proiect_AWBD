package bankingService.demo.adapter.in.web.mapper;

import bankingService.demo.adapter.in.web.dto.beneficiary.BeneficiaryResponse;
import bankingService.demo.adapter.in.web.dto.beneficiary.CreateBeneficiaryRequest;
import bankingService.demo.adapter.in.web.dto.beneficiary.UpdateBeneficiaryRequest;
import bankingService.demo.domain.model.Beneficiary;
import org.springframework.stereotype.Component;

@Component
public class BeneficiaryWebMapper {

    public Beneficiary toDomain(CreateBeneficiaryRequest request) {
        return Beneficiary.builder()
                .ownerAccountId(request.ownerAccountId())
                .beneficiaryName(request.beneficiaryName())
                .beneficiaryIban(request.beneficiaryIban())
                .nickname(request.nickname())
                .build();
    }

    public Beneficiary toDomain(UpdateBeneficiaryRequest request) {
        return Beneficiary.builder()
                .beneficiaryName(request.beneficiaryName())
                .nickname(request.nickname())
                .build();
    }

    public BeneficiaryResponse toResponse(Beneficiary domain) {
        return new BeneficiaryResponse(
                domain.getId(),
                domain.getOwnerAccountId(),
                domain.getBeneficiaryName(),
                domain.getBeneficiaryIban(),
                domain.getNickname(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}
