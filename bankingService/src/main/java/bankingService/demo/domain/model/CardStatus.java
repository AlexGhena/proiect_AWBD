package bankingService.demo.domain.model;

public enum CardStatus {
    ACTIVE,
    BLOCKED,
    EXPIRED,
    /** Terminal: set only via the dedicated report-lost action, never reversible through updateCard. */
    LOST_STOLEN
}
