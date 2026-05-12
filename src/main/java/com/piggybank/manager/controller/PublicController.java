package com.piggybank.manager.controller;

import com.piggybank.manager.dto.ApiResponse;
import com.piggybank.manager.dto.BillDtos;
import com.piggybank.manager.service.BorrowService;
import com.piggybank.manager.service.DepositService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicController {
    private final BorrowService borrowService;
    private final DepositService depositService;

    public PublicController(BorrowService borrowService, DepositService depositService) {
        this.borrowService = borrowService;
        this.depositService = depositService;
    }

    @GetMapping("/borrow-links/{token}")
    public ApiResponse<?> borrowLink(@PathVariable String token) {
        return ApiResponse.ok(borrowService.linkStatus(token));
    }

    @PostMapping("/borrow-links/{token}/submit")
    public ApiResponse<?> submit(@PathVariable String token, @Valid @RequestBody BillDtos.PublicBorrowRequest request) {
        return ApiResponse.ok(borrowService.submitPublic(token, request));
    }

    @GetMapping("/withdrawal-links/{token}")
    public ApiResponse<?> withdrawalLink(@PathVariable String token) {
        return ApiResponse.ok(depositService.withdrawalLinkStatus(token));
    }

    @PostMapping("/withdrawal-links/{token}/submit")
    public ApiResponse<?> submitWithdrawal(@PathVariable String token, @Valid @RequestBody BillDtos.PublicWithdrawalRequest request) {
        return ApiResponse.ok(depositService.submitPublicWithdrawal(token, request));
    }
}
