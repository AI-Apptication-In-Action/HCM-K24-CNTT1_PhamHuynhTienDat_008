package com.banking.models.repositories;

import com.banking.models.entities.TermDeposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TermDepositRepository extends JpaRepository<TermDeposit, Long> {
    List<TermDeposit> findByCustomerId(Long customerId);
    List<TermDeposit> findByBankAccountId(Long bankAccountId);
}
