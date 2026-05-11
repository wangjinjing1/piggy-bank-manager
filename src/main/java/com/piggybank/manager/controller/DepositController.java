package com.piggybank.manager.controller;

import com.piggybank.manager.dto.ApiResponse;
import com.piggybank.manager.dto.BillDtos;
import com.piggybank.manager.service.DepositService;
import com.piggybank.manager.util.AuthContext;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deposits")
public class DepositController {
    private final DepositService depositService;

    public DepositController(DepositService depositService) {
        this.depositService = depositService;
    }

    @GetMapping
    public ApiResponse<?> list(@ModelAttribute BillDtos.DepositListQuery query) {
        return ApiResponse.ok(depositService.list(AuthContext.get().getId(), query));
    }

    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody BillDtos.DepositRequest request) {
        return ApiResponse.ok(depositService.create(AuthContext.get().getId(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable Long id, @Valid @RequestBody BillDtos.DepositRequest request) {
        return ApiResponse.ok(depositService.update(AuthContext.get().getId(), id, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> detail(@PathVariable Long id,
                                 @ModelAttribute BillDtos.ReportQuery query) {
        return ApiResponse.ok(depositService.detail(AuthContext.get().getId(), id, query.getPage(), query.getSize()));
    }

    @PostMapping("/{id}/withdrawals")
    public ApiResponse<?> withdraw(@PathVariable Long id, @Valid @RequestBody BillDtos.WithdrawalRequest request) {
        return ApiResponse.ok(depositService.withdraw(AuthContext.get().getId(), id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<?> status(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok(depositService.updateStatus(AuthContext.get().getId(), id, body.get("status")));
    }

    @PatchMapping("/{id}/owner")
    public ApiResponse<?> owner(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        requireAdmin();
        return ApiResponse.ok(depositService.updateOwner(id, body.get("ownerUserId")));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable Long id) {
        depositService.delete(AuthContext.get().getId(), id);
        return ApiResponse.ok(true);
    }

    private void requireAdmin() {
        if (AuthContext.get() == null || !AuthContext.get().isAdmin()) {
            throw new IllegalArgumentException("需要管理员权限");
        }
    }
}
