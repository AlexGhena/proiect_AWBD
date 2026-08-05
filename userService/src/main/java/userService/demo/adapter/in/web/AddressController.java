package userService.demo.adapter.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import userService.demo.adapter.in.web.dto.address.AddressResponse;
import userService.demo.adapter.in.web.dto.address.CreateAddressRequest;
import userService.demo.adapter.in.web.dto.address.UpdateAddressRequest;
import userService.demo.adapter.in.web.mapper.AddressWebMapper;
import userService.demo.domain.model.Address;
import userService.demo.domain.port.in.AddressUseCase;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class AddressController {

    private final AddressUseCase addressUseCase;
    private final AddressWebMapper mapper;

    @PostMapping("/api/addresses")
    public ResponseEntity<AddressResponse> create(@Valid @RequestBody CreateAddressRequest request) {
        Address created = addressUseCase.createAddress(mapper.toDomain(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(created));
    }

    @GetMapping("/api/addresses/{id}")
    public ResponseEntity<AddressResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(mapper.toResponse(addressUseCase.getAddress(id)));
    }

    @GetMapping("/api/addresses")
    public ResponseEntity<PagedModel<AddressResponse>> list(Pageable pageable) {
        Page<AddressResponse> page = addressUseCase.listAddresses(pageable).map(mapper::toResponse);
        return ResponseEntity.ok(new PagedModel<>(page));
    }

    @GetMapping("/api/profiles/{profileId}/addresses")
    public ResponseEntity<List<AddressResponse>> listByProfile(@PathVariable UUID profileId) {
        List<AddressResponse> addresses = addressUseCase.listAddressesByProfile(profileId).stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(addresses);
    }

    @PutMapping("/api/addresses/{id}")
    public ResponseEntity<AddressResponse> update(@PathVariable UUID id,
                                                    @Valid @RequestBody UpdateAddressRequest request) {
        Address updated = addressUseCase.updateAddress(id, mapper.toDomain(request));
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @DeleteMapping("/api/addresses/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        addressUseCase.deleteAddress(id);
        return ResponseEntity.noContent().build();
    }
}
