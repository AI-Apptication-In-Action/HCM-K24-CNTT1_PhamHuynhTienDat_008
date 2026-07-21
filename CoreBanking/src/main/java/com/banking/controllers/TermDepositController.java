package com.banking.controllers;

import com.banking.advice.ApiResponse;
import com.banking.models.dto.CreateTermDepositRequest;
import com.banking.models.dto.SettleTermDepositRequest;
import com.banking.models.dto.TermDepositResponse;
import com.banking.models.services.TermDepositService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/term-deposits")
@RequiredArgsConstructor
public class TermDepositController {

    private final TermDepositService termDepositService;

    /**
     * API Mở sổ tiết kiệm mới
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TermDepositResponse>> openTermDeposit(@Valid @RequestBody CreateTermDepositRequest request) {
        TermDepositResponse response = termDepositService.openTermDeposit(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Mở sổ tiết kiệm thành công"));
    }

    /**
     * API Tất toán sổ tiết kiệm (Term Deposit Settlement)
     */
    @PostMapping("/{id}/settle")
    public ResponseEntity<ApiResponse<TermDepositResponse>> settleTermDeposit(
            @PathVariable("id") Long id,
            @RequestBody(required = false) SettleTermDepositRequest request) {
        TermDepositResponse response = termDepositService.settleTermDeposit(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Tất toán sổ tiết kiệm thành công"));
    }

    /**
     * API Lấy thông tin chi tiết 1 sổ tiết kiệm
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TermDepositResponse>> getTermDepositById(@PathVariable("id") Long id) {
        TermDepositResponse response = termDepositService.getTermDepositById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin sổ tiết kiệm thành công"));
    }

    /**
     * API Lấy danh sách tất cả sổ tiết kiệm
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TermDepositResponse>>> getAllTermDeposits() {
        List<TermDepositResponse> responseList = termDepositService.getAllTermDeposits();
        return ResponseEntity.ok(ApiResponse.success(responseList, "Lấy danh sách sổ tiết kiệm thành công"));
    }
}
