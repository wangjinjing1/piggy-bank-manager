package com.piggybank.manager.controller;

import com.piggybank.manager.dto.ApiResponse;
import com.piggybank.manager.dto.BillDtos;
import com.piggybank.manager.service.BorrowService;
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
@RequestMapping("/api/borrows")
public class BorrowController {
    private final BorrowService borrowService;

    public BorrowController(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    @GetMapping
    public ApiResponse<?> list(@ModelAttribute BillDtos.BorrowListQuery query) {
        return ApiResponse.ok(borrowService.list(AuthContext.get().getId(), query));
    }

    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody BillDtos.BorrowRequest request) {
        return ApiResponse.ok(borrowService.create(AuthContext.get().getId(), request, "MANUAL", "APPROVED"));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable Long id, @Valid @RequestBody BillDtos.BorrowRequest request) {
        return ApiResponse.ok(borrowService.update(AuthContext.get().getId(), id, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> detail(@PathVariable Long id,
                                 @ModelAttribute BillDtos.ReportQuery query) {
        return ApiResponse.ok(borrowService.detail(AuthContext.get().getId(), id, query.getPage(), query.getSize()));
    }

    @PostMapping("/{id}/repayments")
    public ApiResponse<?> repay(@PathVariable Long id, @Valid @RequestBody BillDtos.RepaymentRequest request) {
        return ApiResponse.ok(borrowService.repay(AuthContext.get().getId(), id, request));
    }

    @DeleteMapping("/{id}/repayments/{repaymentId}")
    public ApiResponse<?> deleteRepayment(@PathVariable Long id, @PathVariable Long repaymentId) {
        borrowService.deleteRepayment(AuthContext.get().getId(), id, repaymentId);
        return ApiResponse.ok(true);
    }

    @PostMapping("/links")
    public ApiResponse<?> createLink() {
        return ApiResponse.ok(Map.of("url", borrowService.createLink(AuthContext.get().getId())));
    }

    @GetMapping("/overdue")
    public ApiResponse<?> overdue() {
        return ApiResponse.ok(borrowService.overdueGroups(AuthContext.get().getId()));
    }

    @PostMapping("/overdue/remind")
    public ApiResponse<?> remindOverdue(@RequestBody Map<String, String> body) {
        borrowService.sendOverdueReminder(AuthContext.get().getId(), body.get("email"));
        return ApiResponse.ok(true);
    }

    @PatchMapping("/{id}/approve")
    public ApiResponse<?> approve(@PathVariable Long id) {
        return ApiResponse.ok(borrowService.approve(AuthContext.get().getId(), id));
    }

    @PostMapping("/{id}/audit-mail")
    public ApiResponse<?> auditMail(@PathVariable Long id) {
        return ApiResponse.ok(borrowService.sendAuditMail(AuthContext.get().getId(), id));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<?> status(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok(borrowService.updateStatus(AuthContext.get().getId(), id, body.get("status")));
    }

    @PatchMapping("/{id}/owner")
    public ApiResponse<?> owner(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        requireAdmin();
        return ApiResponse.ok(borrowService.updateOwner(id, body.get("ownerUserId")));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable Long id) {
        borrowService.delete(AuthContext.get().getId(), id);
        return ApiResponse.ok(true);
    }

    private void requireAdmin() {
        if (AuthContext.get() == null || !AuthContext.get().isAdmin()) {
            throw new IllegalArgumentException("需要管理员权限");
        }
    }
}
