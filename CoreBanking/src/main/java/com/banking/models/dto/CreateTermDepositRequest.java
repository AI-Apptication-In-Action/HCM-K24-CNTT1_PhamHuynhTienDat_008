package com.banking.models.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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
public class CreateTermDepositRequest {

    private Long customerId;

    private Long bankAccountId;

    @NotNull(message = "Số tiền gửi không được để trống")
    @DecimalMin(value = "0.01", message = "Số tiền gửi phải lớn hơn 0")
    private BigDecimal principalAmount;

    @NotNull(message = "Kỳ hạn gửi không được để trống")
    private Integer termMonths; // 1, 6, 12 tháng

    @NotNull(message = "Lãi suất không được để trống")
    private BigDecimal interestRate; // Ví dụ: 0.065 (tương đương 6.5%/năm)

    private LocalDate depositDate; // Nếu null sẽ tự lấy ngày hiện tại
}
