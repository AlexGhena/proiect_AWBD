package userService.demo.adapter.in.web.dto.user;

/** Response for {@code POST /api/users/{id}/approve}: the now-active user plus its new IBAN. */
public record UserApprovalResponse(UserResponse user, String iban) {
}
