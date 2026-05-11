package com.piggybank.manager.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("borrow_repayment")
public class BorrowRepayment {
    private Long id;
    private Long borrowBillId;
    private Long ownerUserId;
    private BigDecimal amount;
    private LocalDate repaymentDate;
    private String remark;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBorrowBillId() { return borrowBillId; }
    public void setBorrowBillId(Long borrowBillId) { this.borrowBillId = borrowBillId; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getRepaymentDate() { return repaymentDate; }
    public void setRepaymentDate(LocalDate repaymentDate) { this.repaymentDate = repaymentDate; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
