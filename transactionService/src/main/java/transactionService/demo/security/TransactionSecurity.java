package transactionService.demo.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import transactionService.demo.domain.port.out.BankTransactionRepositoryPort;
import transactionService.demo.domain.port.out.ScheduledTransactionRepositoryPort;

import java.util.UUID;

/**
 * Resource-ownership checks referenced from {@code @PreAuthorize} as {@code @transactionSecurity}.
 *
 * <p>A transaction is owned by whoever owns the accounts on either end of it, and those accounts
 * live in bankingService. Ownership is therefore resolved by forwarding the caller's own token to
 * that service rather than by trusting anything in the request.
 */
@Component("transactionSecurity")
@RequiredArgsConstructor
public class TransactionSecurity {

    private final BankTransactionRepositoryPort transactionRepositoryPort;
    private final ScheduledTransactionRepositoryPort scheduledTransactionRepositoryPort;
    private final BankingAccountClient bankingAccountClient;

    /** Touching either side of a transfer requires access to the account on that side. */
    public boolean ownsEitherAccount(UUID sourceAccountId, UUID destinationAccountId) {
        return bankingAccountClient.callerCanAccessAccount(sourceAccountId)
                || bankingAccountClient.callerCanAccessAccount(destinationAccountId);
    }

    @Transactional(readOnly = true)
    public boolean ownsTransaction(UUID transactionId) {
        return transactionRepositoryPort.findById(transactionId)
                .map(transaction -> ownsEitherAccount(
                        transaction.getSourceAccountId(), transaction.getDestinationAccountId()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean ownsScheduledTransaction(UUID scheduledTransactionId) {
        return scheduledTransactionRepositoryPort.findById(scheduledTransactionId)
                .map(scheduled -> ownsEitherAccount(
                        scheduled.getSourceAccountId(), scheduled.getDestinationAccountId()))
                .orElse(false);
    }
}
