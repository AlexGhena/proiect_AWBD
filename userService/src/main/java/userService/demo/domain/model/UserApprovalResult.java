package userService.demo.domain.model;

import lombok.Builder;

/** Outcome of approving a pending registration: the now-active user plus the IBAN bankingService minted for it. */
@Builder
public record UserApprovalResult(AppUser user, String iban) {
}
