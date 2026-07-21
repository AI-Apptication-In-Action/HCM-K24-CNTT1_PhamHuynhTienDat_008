package com.banking.models.services;

import com.banking.exceptions.BusinessException;
import com.banking.models.constant.TermDepositStatus;
import com.banking.models.dto.CreateTermDepositRequest;
import com.banking.models.dto.SettleTermDepositRequest;
import com.banking.models.dto.TermDepositResponse;
import com.banking.models.entities.BankAccount;
import com.banking.models.entities.Customer;
import com.banking.models.entities.TermDeposit;
import com.banking.models.repositories.BankAccountRepository;
import com.banking.models.repositories.CustomerRepository;
import com.banking.models.repositories.TermDepositRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TermDepositServiceImpl implements TermDepositService {

    private final TermDepositRepository termDepositRepository;
    private final CustomerRepository customerRepository;
    private final BankAccountRepository bankAccountRepository;

    // Lãi suất không kỳ hạn mặc định khi rút trước hạn: 0.1%/năm (0.001)
    private static final BigDecimal DEMAND_INTEREST_RATE = new BigDecimal("0.001");

    @Override
    @Transactional
    public TermDepositResponse openTermDeposit(CreateTermDepositRequest request) {
        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId()).orElse(null);
        }

        BankAccount bankAccount = null;
        if (request.getBankAccountId() != null) {
            bankAccount = bankAccountRepository.findById(request.getBankAccountId()).orElse(null);
            if (customer == null && bankAccount != null) {
                customer = bankAccount.getCustomer();
            }
        }

        LocalDate depositDate = request.getDepositDate() != null ? request.getDepositDate() : LocalDate.now();
        LocalDate maturityDate = depositDate.plusMonths(request.getTermMonths());

        TermDeposit termDeposit = TermDeposit.builder()
                .customer(customer)
                .bankAccount(bankAccount)
                .principalAmount(request.getPrincipalAmount())
                .interestRate(request.getInterestRate())
                .termMonths(request.getTermMonths())
                .depositDate(depositDate)
                .maturityDate(maturityDate)
                .status(TermDepositStatus.ACTIVE)
                .build();

        TermDeposit savedDeposit = termDepositRepository.save(termDeposit);
        return mapToResponse(savedDeposit, null, false);
    }

    @Override
    @Transactional
    public TermDepositResponse settleTermDeposit(Long id, SettleTermDepositRequest request) {
        TermDeposit deposit = termDepositRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND.value(), "Không tìm thấy sổ tiết kiệm với ID: " + id));

        // Ràng buộc 1: Sổ đã tất toán rồi thì không được phép tất toán lại -> Ném ngoại lệ 400 Bad Request
        if (deposit.getStatus() == TermDepositStatus.SETTLED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), "Sổ tiết kiệm đã được tất toán trước đó");
        }

        // Xác định ngày tất toán thực tế
        LocalDate settlementDate = (request != null && request.getSettlementDate() != null)
                ? request.getSettlementDate()
                : LocalDate.now();

        // Tính số ngày gửi thực tế
        long actualDays = ChronoUnit.DAYS.between(deposit.getDepositDate(), settlementDate);
        if (actualDays < 0) {
            actualDays = 0;
        }

        // Logic rẽ nhánh lãi suất:
        // - Rút trước ngày đáo hạn (dù chỉ 1 ngày): Lãi suất không kỳ hạn (0.1%/năm)
        // - Rút đúng hoặc sau ngày đáo hạn: Nguyên lãi suất có kỳ hạn ban đầu
        boolean isEarlySettlement = settlementDate.isBefore(deposit.getMaturityDate());
        BigDecimal appliedInterestRate = isEarlySettlement ? DEMAND_INTEREST_RATE : deposit.getInterestRate();

        // Công thức tính lãi chuẩn xác: (Gốc * Lãi suất * Số ngày gửi thực tế) / 365
        BigDecimal interestEarned = deposit.getPrincipalAmount()
                .multiply(appliedInterestRate)
                .multiply(BigDecimal.valueOf(actualDays))
                .divide(BigDecimal.valueOf(365), 2, RoundingMode.HALF_UP);

        BigDecimal settlementAmount = deposit.getPrincipalAmount().add(interestEarned);

        // Cập nhật trạng thái và dữ liệu tất toán
        deposit.setSettlementDate(settlementDate);
        deposit.setInterestEarned(interestEarned);
        deposit.setSettlementAmount(settlementAmount);
        deposit.setStatus(TermDepositStatus.SETTLED);

        // Nút thắt nghiệp vụ Core Banking: Cộng tiền tất toán vào tài khoản liên kết nếu có
        if (deposit.getBankAccount() != null) {
            BankAccount account = deposit.getBankAccount();
            account.setBalance(account.getBalance().add(settlementAmount));
            bankAccountRepository.save(account);
        }

        TermDeposit updatedDeposit = termDepositRepository.save(deposit);
        return mapToResponse(updatedDeposit, appliedInterestRate, isEarlySettlement);
    }

    @Override
    @Transactional(readOnly = true)
    public TermDepositResponse getTermDepositById(Long id) {
        TermDeposit deposit = termDepositRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND.value(), "Không tìm thấy sổ tiết kiệm với ID: " + id));

        boolean isEarlySettlement = false;
        BigDecimal appliedInterestRate = deposit.getInterestRate();

        if (deposit.getStatus() == TermDepositStatus.SETTLED && deposit.getSettlementDate() != null) {
            isEarlySettlement = deposit.getSettlementDate().isBefore(deposit.getMaturityDate());
            appliedInterestRate = isEarlySettlement ? DEMAND_INTEREST_RATE : deposit.getInterestRate();
        }

        return mapToResponse(deposit, appliedInterestRate, isEarlySettlement);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TermDepositResponse> getAllTermDeposits() {
        return termDepositRepository.findAll().stream()
                .map(deposit -> {
                    boolean isEarly = false;
                    BigDecimal rate = deposit.getInterestRate();
                    if (deposit.getStatus() == TermDepositStatus.SETTLED && deposit.getSettlementDate() != null) {
                        isEarly = deposit.getSettlementDate().isBefore(deposit.getMaturityDate());
                        rate = isEarly ? DEMAND_INTEREST_RATE : deposit.getInterestRate();
                    }
                    return mapToResponse(deposit, rate, isEarly);
                })
                .collect(Collectors.toList());
    }

    private TermDepositResponse mapToResponse(TermDeposit deposit, BigDecimal appliedInterestRate, Boolean isEarlySettlement) {
        return TermDepositResponse.builder()
                .id(deposit.getId())
                .customerId(deposit.getCustomer() != null ? deposit.getCustomer().getId() : null)
                .bankAccountId(deposit.getBankAccount() != null ? deposit.getBankAccount().getId() : null)
                .principalAmount(deposit.getPrincipalAmount())
                .interestRate(deposit.getInterestRate())
                .termMonths(deposit.getTermMonths())
                .depositDate(deposit.getDepositDate())
                .maturityDate(deposit.getMaturityDate())
                .settlementDate(deposit.getSettlementDate())
                .interestEarned(deposit.getInterestEarned())
                .settlementAmount(deposit.getSettlementAmount())
                .status(deposit.getStatus())
                .appliedInterestRate(appliedInterestRate != null ? appliedInterestRate : deposit.getInterestRate())
                .isEarlySettlement(isEarlySettlement)
                .build();
    }
}
