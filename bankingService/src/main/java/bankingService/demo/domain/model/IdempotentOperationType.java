package bankingService.demo.domain.model;

public enum IdempotentOperationType {
    DEBIT,
    CREDIT,
    COMPENSATE
}
