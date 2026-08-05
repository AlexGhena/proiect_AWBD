package bankingService.demo.adapter.in.web;

import bankingService.demo.adapter.in.web.dto.beneficiary.BeneficiaryResponse;
import bankingService.demo.adapter.in.web.dto.beneficiary.CreateBeneficiaryRequest;
import bankingService.demo.adapter.in.web.dto.beneficiary.UpdateBeneficiaryRequest;
import bankingService.demo.adapter.in.web.mapper.BeneficiaryWebMapper;
import bankingService.demo.domain.model.Beneficiary;
import bankingService.demo.domain.port.in.BeneficiaryUseCase;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
public class BeneficiaryController {

    private final BeneficiaryUseCase beneficiaryUseCase;
    private final BeneficiaryWebMapper mapper;

    public BeneficiaryController(BeneficiaryUseCase beneficiaryUseCase, BeneficiaryWebMapper mapper) {
        this.beneficiaryUseCase = beneficiaryUseCase;
        this.mapper = mapper;
    }

    @PostMapping("/api/beneficiaries")
    public ResponseEntity<BeneficiaryResponse> create(@Valid @RequestBody CreateBeneficiaryRequest request) {
        Beneficiary created = beneficiaryUseCase.createBeneficiary(mapper.toDomain(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(created));
    }

    @GetMapping("/api/beneficiaries/{id}")
    public ResponseEntity<BeneficiaryResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(mapper.toResponse(beneficiaryUseCase.getBeneficiary(id)));
    }

    @GetMapping("/api/beneficiaries")
    public ResponseEntity<PagedModel<BeneficiaryResponse>> list(Pageable pageable) {
        Page<BeneficiaryResponse> page = beneficiaryUseCase.listBeneficiaries(pageable).map(mapper::toResponse);
        return ResponseEntity.ok(new PagedModel<>(page));
    }

    @GetMapping("/api/accounts/{accountId}/beneficiaries")
    public ResponseEntity<List<BeneficiaryResponse>> listByAccount(@PathVariable UUID accountId) {
        List<BeneficiaryResponse> beneficiaries = beneficiaryUseCase.listBeneficiariesByAccount(accountId).stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(beneficiaries);
    }

    @PutMapping("/api/beneficiaries/{id}")
    public ResponseEntity<BeneficiaryResponse> update(@PathVariable UUID id,
                                                        @Valid @RequestBody UpdateBeneficiaryRequest request) {
        Beneficiary updated = beneficiaryUseCase.updateBeneficiary(id, mapper.toDomain(request));
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @DeleteMapping("/api/beneficiaries/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        beneficiaryUseCase.deleteBeneficiary(id);
        return ResponseEntity.noContent().build();
    }
}
