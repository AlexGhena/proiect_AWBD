package bankingService.demo.adapter.out.persistence.entity;

import bankingService.demo.domain.model.CardStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bank_cards")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankCardJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private BankAccountJpaEntity account;

    @Column(name = "card_reference", nullable = false, unique = true, length = 64)
    private String cardReference;

    @Column(name = "last_four", nullable = false, length = 4)
    private String lastFour;

    @Column(name = "cardholder_name", nullable = false, length = 100)
    private String cardholderName;

    @Column(name = "expiry_month", nullable = false)
    private Short expiryMonth;

    @Column(name = "expiry_year", nullable = false)
    private Short expiryYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CardStatus status;

    /** AES-256-GCM ciphertext, base64-encoded with a random IV prefix. NULL for pre-feature cards. */
    @Column(name = "card_number_encrypted")
    private String cardNumberEncrypted;

    @Column(name = "cvv_encrypted")
    private String cvvEncrypted;

    @Column(name = "pin_encrypted")
    private String pinEncrypted;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
