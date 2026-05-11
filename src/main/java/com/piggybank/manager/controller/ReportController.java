package com.piggybank.manager.controller;

import com.piggybank.manager.dto.ApiResponse;
import com.piggybank.manager.dto.BillDtos;
import com.piggybank.manager.service.BorrowService;
import com.piggybank.manager.service.DepositService;
import com.piggybank.manager.util.AuthContext;
import java.nio.charset.StandardCharsets;
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
            return ApiResponse.ok(depositService.reportGroups(ownerId, query.getStartDate(), query.getEndDate(), query.getName(), query.getPage(), query.getSize()));
        }
        return ApiResponse.ok(borrowService.reportGroups(ownerId, query.getStartDate(), query.getEndDate(), query.getName(), query.getPage(), query.getSize()));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@ModelAttribute BillDtos.ReportQuery query) {
        Long ownerId = AuthContext.get().getId();
        String csv;
        String filename;
        if ("DEPOSIT".equalsIgnoreCase(query.getType())) {
            filename = "deposit-report.csv";
            csv = "存款人,金额,已取金额,剩余金额,银行,存款日期,到期日期,状态,备注\n" + depositService.report(ownerId, query.getStartDate(), query.getEndDate(), query.getName()).stream()
                    .map(b -> safe(b.getDepositorName()) + "," + b.getAmount() + "," + b.getWithdrawnAmount() + "," + b.getRemainingAmount() + "," + safe(b.getBank()) + "," + b.getDepositDate() + "," + b.getDueDate() + "," + statusText(b.getStatus()) + "," + safe(b.getRemark()))
                    .reduce("", (a, b) -> a + b + "\n");
        } else {
            filename = "borrow-report.csv";
            csv = "借款人,手机号,邮箱,借款金额,已还金额,待还金额,借款日期,还款日期,状态,审核状态,备注\n" + borrowService.report(ownerId, query.getStartDate(), query.getEndDate(), query.getName()).stream()
                    .map(b -> safe(b.getBorrowerName()) + "," + safe(b.getPhone()) + "," + safe(b.getEmail()) + "," + b.getAmount() + "," + b.getPaidAmount() + "," + b.getRemainingAmount() + "," + b.getBorrowDate() + "," + b.getDueDate() + "," + statusText(b.getStatus()) + "," + auditText(b.getAuditStatus()) + "," + safe(b.getRemark()))
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
