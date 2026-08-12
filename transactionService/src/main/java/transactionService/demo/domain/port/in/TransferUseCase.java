package transactionService.demo.domain.port.in;

import transactionService.demo.domain.model.BankTransaction;

/**
 * The transfer Saga: debits the source account, credits the destination account, and compensates
 * the debit if the credit fails. See {@code docs/BACKEND_ARCHITECTURE.md} section 9.
 */
public interface TransferUseCase {

    /**
     * {@code transaction} carries only the caller-supplied fields (type TRANSFER, status null);
     * the returned transaction reflects the saga's final state (COMPLETED, FAILED or COMPENSATED).
     */
    BankTransaction executeTransfer(BankTransaction transaction);
}
