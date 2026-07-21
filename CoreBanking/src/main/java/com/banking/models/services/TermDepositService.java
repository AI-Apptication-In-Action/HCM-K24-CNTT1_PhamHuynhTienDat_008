package com.banking.models.services;

import com.banking.models.dto.CreateTermDepositRequest;
import com.banking.models.dto.SettleTermDepositRequest;
import com.banking.models.dto.TermDepositResponse;

import java.util.List;

public interface TermDepositService {
    TermDepositResponse openTermDeposit(CreateTermDepositRequest request);
    TermDepositResponse settleTermDeposit(Long id, SettleTermDepositRequest request);
    TermDepositResponse getTermDepositById(Long id);
    List<TermDepositResponse> getAllTermDeposits();
}
