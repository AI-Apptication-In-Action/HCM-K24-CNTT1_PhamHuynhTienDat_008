package com.banking.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettleTermDepositRequest {
    private LocalDate settlementDate; // Ngày tất toán thực tế. Nếu null sẽ mặc định là LocalDate.now()
}
