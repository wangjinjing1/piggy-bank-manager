package com.piggybank.manager.controller;

import com.piggybank.manager.dto.ApiResponse;
import com.piggybank.manager.dto.BillDtos;
import com.piggybank.manager.service.BorrowService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicController {
    private final BorrowService borrowService;

    public PublicController(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    @PostMapping("/borrow-links/{token}/submit")
    public ApiResponse<?> submit(@PathVariable String token, @Valid @RequestBody BillDtos.BorrowRequest request) {
        return ApiResponse.ok(borrowService.submitPublic(token, request));
    }
}
