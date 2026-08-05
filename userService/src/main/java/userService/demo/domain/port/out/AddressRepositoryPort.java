package userService.demo.domain.port.out;

import userService.demo.domain.model.Address;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AddressRepositoryPort {

    Address save(Address address);

    Optional<Address> findById(UUID id);

    boolean existsById(UUID id);

    Page<Address> findAll(Pageable pageable);

    List<Address> findByProfileId(UUID profileId);

    void deleteById(UUID id);
}
