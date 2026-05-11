package com.piggybank.manager.controller;

import com.piggybank.manager.domain.BorrowBill;
import com.piggybank.manager.domain.DepositBill;
import com.piggybank.manager.dto.ApiResponse;
import com.piggybank.manager.dto.BillDtos;
import com.piggybank.manager.service.BorrowService;
import com.piggybank.manager.service.DepositService;
import com.piggybank.manager.util.AuthContext;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final BorrowService borrowService;
    private final DepositService depositService;

    public ReportController(BorrowService borrowService, DepositService depositService) {
        this.borrowService = borrowService;
        this.depositService = depositService;
    }

    @GetMapping
    public ApiResponse<?> report(@ModelAttribute BillDtos.ReportQuery query) {
        Long ownerId = AuthContext.get().getId();
        if ("DEPOSIT".equalsIgnoreCase(query.getType())) {
            List<DepositBill> items = depositService.report(ownerId, query.getStartDate(), query.getEndDate());
            return ApiResponse.ok(Map.of("total", depositService.sum(items), "items", items));
        }
        List<BorrowBill> items = borrowService.report(ownerId, query.getStartDate(), query.getEndDate(), query.getName());
        return ApiResponse.ok(Map.of("total", borrowService.sum(items), "items", items));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@ModelAttribute BillDtos.ReportQuery query) {
        Long ownerId = AuthContext.get().getId();
        String csv;
        String filename;
        if ("DEPOSIT".equalsIgnoreCase(query.getType())) {
            filename = "deposit-report.csv";
            csv = "金额,银行,存款日期,到期日期,状态\n" + depositService.report(ownerId, query.getStartDate(), query.getEndDate()).stream()
                    .map(b -> b.getAmount() + "," + safe(b.getBank()) + "," + b.getDepositDate() + "," + b.getDueDate() + "," + statusText(b.getStatus()))
                    .reduce("", (a, b) -> a + b + "\n");
        } else {
            filename = "borrow-report.csv";
            csv = "借款人,手机号,邮箱,金额,借款日期,还款日期,状态,审核状态\n" + borrowService.report(ownerId, query.getStartDate(), query.getEndDate(), query.getName()).stream()
                    .map(b -> safe(b.getBorrowerName()) + "," + safe(b.getPhone()) + "," + safe(b.getEmail()) + "," + b.getAmount() + "," + b.getBorrowDate() + "," + b.getDueDate() + "," + statusText(b.getStatus()) + "," + auditText(b.getAuditStatus()))
                    .reduce("", (a, b) -> a + b + "\n");
        }
        byte[] bytes = ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(bytes);
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String statusText(String status) {
        return "VOID".equals(status) ? "作废" : "正常";
    }

    private String auditText(String auditStatus) {
        if ("PENDING".equals(auditStatus)) {
            return "待审核";
        }
        if ("REJECTED".equals(auditStatus)) {
            return "已拒绝";
        }
        return "已通过";
    }
}
