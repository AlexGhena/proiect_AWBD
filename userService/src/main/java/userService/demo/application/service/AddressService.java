package userService.demo.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import userService.demo.domain.exception.ResourceNotFoundException;
import userService.demo.domain.model.Address;
import userService.demo.domain.model.AddressLabel;
import userService.demo.domain.port.in.AddressUseCase;
import userService.demo.domain.port.out.AddressRepositoryPort;
import userService.demo.domain.port.out.ProfileRepositoryPort;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AddressService implements AddressUseCase {

    private final AddressRepositoryPort addressRepositoryPort;
    private final ProfileRepositoryPort profileRepositoryPort;

    @Override
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.ownsProfile(#address.profileId)")
    public Address createAddress(Address address) {
        if (!profileRepositoryPort.existsById(address.getProfileId())) {
            throw new ResourceNotFoundException("Profile " + address.getProfileId() + " not found");
        }
        address.setId(null);
        if (address.getLabel() == null) {
            address.setLabel(AddressLabel.HOME);
        }
        if (address.getIsDefault() == null) {
            address.setIsDefault(false);
        }
        Address saved = addressRepositoryPort.save(address);
        log.info("Address created with id={}, profileId={}", saved.getId(), saved.getProfileId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.ownsAddress(#id)")
    public Address getAddress(UUID id) {
        return addressRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Address " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<Address> listAddresses(Pageable pageable) {
        log.debug("Listing addresses with pageable={}", pageable);
        return addressRepositoryPort.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.ownsProfile(#profileId)")
    public List<Address> listAddressesByProfile(UUID profileId) {
        if (!profileRepositoryPort.existsById(profileId)) {
            throw new ResourceNotFoundException("Profile " + profileId + " not found");
        }
        return addressRepositoryPort.findByProfileId(profileId);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.ownsAddress(#id)")
    public Address updateAddress(UUID id, Address updates) {
        Address existing = getAddress(id);
        if (updates.getLabel() != null) {
            existing.setLabel(updates.getLabel());
        }
        if (updates.getStreet() != null) {
            existing.setStreet(updates.getStreet());
        }
        if (updates.getCity() != null) {
            existing.setCity(updates.getCity());
        }
        if (updates.getPostalCode() != null) {
            existing.setPostalCode(updates.getPostalCode());
        }
        if (updates.getCountry() != null) {
            existing.setCountry(updates.getCountry());
        }
        if (updates.getIsDefault() != null) {
            existing.setIsDefault(updates.getIsDefault());
        }
        Address saved = addressRepositoryPort.save(existing);
        log.info("Address updated with id={}", saved.getId());
        return saved;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.ownsAddress(#id)")
    public void deleteAddress(UUID id) {
        if (!addressRepositoryPort.existsById(id)) {
            throw new ResourceNotFoundException("Address " + id + " not found");
        }
        addressRepositoryPort.deleteById(id);
        log.info("Address deleted with id={}", id);
    }
}
