package com.banking.models.entities;

import com.banking.models.constant.TermDepositStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "term_deposits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TermDeposit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id")
    private BankAccount bankAccount;

    @Column(name = "principal_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal principalAmount;

    // Lãi suất ban đầu (Ví dụ: 0.065 đại diện cho 6.5%/năm)
    @Column(name = "interest_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "term_months", nullable = false)
    private Integer termMonths;

    @Column(name = "deposit_date", nullable = false)
    private LocalDate depositDate;

    @Column(name = "maturity_date", nullable = false)
    private LocalDate maturityDate;

    @Column(name = "settlement_date")
    private LocalDate settlementDate;

    @Column(name = "interest_earned", precision = 19, scale = 4)
    private BigDecimal interestEarned;

    @Column(name = "settlement_amount", precision = 19, scale = 4)
    private BigDecimal settlementAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TermDepositStatus status = TermDepositStatus.ACTIVE;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = TermDepositStatus.ACTIVE;
        }
    }
}
