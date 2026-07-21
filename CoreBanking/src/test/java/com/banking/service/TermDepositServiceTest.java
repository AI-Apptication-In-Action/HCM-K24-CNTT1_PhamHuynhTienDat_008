package com.banking.service;

import com.banking.exceptions.BusinessException;
import com.banking.models.constant.TermDepositStatus;
import com.banking.models.dto.CreateTermDepositRequest;
import com.banking.models.dto.SettleTermDepositRequest;
import com.banking.models.dto.TermDepositResponse;
import com.banking.models.entities.TermDeposit;
import com.banking.models.repositories.BankAccountRepository;
import com.banking.models.repositories.CustomerRepository;
import com.banking.models.repositories.TermDepositRepository;
import com.banking.models.services.TermDepositServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TermDepositServiceTest {

    @Mock
    private TermDepositRepository termDepositRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @InjectMocks
    private TermDepositServiceImpl termDepositService;

    private TermDeposit activeDeposit;

    @BeforeEach
    void setUp() {
        activeDeposit = TermDeposit.builder()
                .id(1L)
                .principalAmount(new BigDecimal("100000000")) // 100,000,000 VND
                .interestRate(new BigDecimal("0.065")) // 6.5%/năm
                .termMonths(6)
                .depositDate(LocalDate.of(2026, 1, 1))
                .maturityDate(LocalDate.of(2026, 7, 1))
                .status(TermDepositStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Tất toán đúng hoặc sau ngày đáo hạn -> Hưởng nguyên lãi suất có kỳ hạn (6.5%/năm)")
    void settleTermDeposit_OnOrAfterMaturityDate_ShouldApplyOriginalInterestRate() {
        when(termDepositRepository.findById(1L)).thenReturn(Optional.of(activeDeposit));
        when(termDepositRepository.save(any(TermDeposit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDate settlementDate = LocalDate.of(2026, 7, 1); // Đúng ngày đáo hạn
        SettleTermDepositRequest request = SettleTermDepositRequest.builder()
                .settlementDate(settlementDate)
                .build();

        TermDepositResponse response = termDepositService.settleTermDeposit(1L, request);

        assertNotNull(response);
        assertEquals(TermDepositStatus.SETTLED, response.getStatus());
        assertFalse(response.getIsEarlySettlement());
        assertEquals(new BigDecimal("0.065"), response.getAppliedInterestRate());
        assertNotNull(response.getInterestEarned());
        assertTrue(response.getInterestEarned().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Tất toán trước ngày đáo hạn (Dù chỉ 1 ngày) -> Hưởng lãi suất không kỳ hạn (0.1%/năm)")
    void settleTermDeposit_BeforeMaturityDate_ShouldApplyDemandInterestRate() {
        when(termDepositRepository.findById(1L)).thenReturn(Optional.of(activeDeposit));
        when(termDepositRepository.save(any(TermDeposit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDate settlementDate = LocalDate.of(2026, 6, 30); // Rút trước 1 ngày
        SettleTermDepositRequest request = SettleTermDepositRequest.builder()
                .settlementDate(settlementDate)
                .build();

        TermDepositResponse response = termDepositService.settleTermDeposit(1L, request);

        assertNotNull(response);
        assertEquals(TermDepositStatus.SETTLED, response.getStatus());
        assertTrue(response.getIsEarlySettlement());
        assertEquals(new BigDecimal("0.001"), response.getAppliedInterestRate()); // 0.1%/năm
    }

    @Test
    @DisplayName("Tất toán lại sổ đã SETTLED -> Ném BusinessException 400 Bad Request với thông báo chính xác")
    void settleTermDeposit_AlreadySettled_ShouldThrowBusinessException400() {
        activeDeposit.setStatus(TermDepositStatus.SETTLED);
        when(termDepositRepository.findById(1L)).thenReturn(Optional.of(activeDeposit));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            termDepositService.settleTermDeposit(1L, new SettleTermDepositRequest());
        });

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getCode());
        assertEquals("Sổ tiết kiệm đã được tất toán trước đó", exception.getMessage());
    }
}
