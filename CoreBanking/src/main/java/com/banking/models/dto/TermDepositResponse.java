package com.banking.models.dto;

import com.banking.models.constant.TermDepositStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TermDepositResponse {
    private Long id;
    private Long customerId;
    private Long bankAccountId;
    private BigDecimal principalAmount;
    private BigDecimal interestRate; // Lãi suất hợp đồng ban đầu
    private Integer termMonths;
    private LocalDate depositDate;
    private LocalDate maturityDate;
    private LocalDate settlementDate;
    private BigDecimal interestEarned;
    private BigDecimal settlementAmount;
    private TermDepositStatus status;
    private BigDecimal appliedInterestRate; // Lãi suất thực tế được áp dụng khi tất toán
    private Boolean isEarlySettlement; // Đánh giá có tất toán trước hạn không
}
