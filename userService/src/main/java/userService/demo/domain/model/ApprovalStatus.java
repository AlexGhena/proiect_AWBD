package userService.demo.domain.model;

/**
 * Lifecycle of a self-registered account: it starts {@code PENDING} and stays disabled and
 * unprivileged until an administrator {@code APPROVE}s it (granting ROLE_USER, enabling login, and
 * provisioning a bank account) or {@code REJECT}s it. Admin-created accounts skip the queue and are
 * created {@code APPROVED} directly.
 */
public enum ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED
}
