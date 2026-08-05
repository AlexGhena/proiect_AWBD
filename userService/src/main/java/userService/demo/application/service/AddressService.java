package userService.demo.application.service;

import userService.demo.domain.exception.ResourceNotFoundException;
import userService.demo.domain.model.Address;
import userService.demo.domain.model.AddressLabel;
import userService.demo.domain.port.in.AddressUseCase;
import userService.demo.domain.port.out.AddressRepositoryPort;
import userService.demo.domain.port.out.ProfileRepositoryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AddressService implements AddressUseCase {

    private final AddressRepositoryPort addressRepositoryPort;
    private final ProfileRepositoryPort profileRepositoryPort;

    public AddressService(AddressRepositoryPort addressRepositoryPort, ProfileRepositoryPort profileRepositoryPort) {
        this.addressRepositoryPort = addressRepositoryPort;
        this.profileRepositoryPort = profileRepositoryPort;
    }

    @Override
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
        return addressRepositoryPort.save(address);
    }

    @Override
    @Transactional(readOnly = true)
    public Address getAddress(UUID id) {
        return addressRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Address " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Address> listAddresses(Pageable pageable) {
        return addressRepositoryPort.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Address> listAddressesByProfile(UUID profileId) {
        if (!profileRepositoryPort.existsById(profileId)) {
            throw new ResourceNotFoundException("Profile " + profileId + " not found");
        }
        return addressRepositoryPort.findByProfileId(profileId);
    }

    @Override
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
        return addressRepositoryPort.save(existing);
    }

    @Override
    public void deleteAddress(UUID id) {
        if (!addressRepositoryPort.existsById(id)) {
            throw new ResourceNotFoundException("Address " + id + " not found");
        }
        addressRepositoryPort.deleteById(id);
    }
}
