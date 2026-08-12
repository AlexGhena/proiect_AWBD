package userService.demo.adapter.out.client.dto;

/**
 * Subset of bankingService's {@code AccountResponse} this client actually needs. Extra fields on
 * the real response (id, userId, balance, status, ...) are simply ignored during deserialization.
 */
public record ProvisionedAccountResponse(String iban) {
}
