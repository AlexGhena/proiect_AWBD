package userService.demo.domain.port.in;

import userService.demo.domain.model.Address;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface AddressUseCase {

    Address createAddress(Address address);

    Address getAddress(UUID id);

    Page<Address> listAddresses(Pageable pageable);

    List<Address> listAddressesByProfile(UUID profileId);

    Address updateAddress(UUID id, Address updates);

    void deleteAddress(UUID id);
}
