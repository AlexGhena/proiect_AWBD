package userService.demo.domain.port.out;

public interface PasswordHasherPort {

    String hash(String rawPassword);
}
