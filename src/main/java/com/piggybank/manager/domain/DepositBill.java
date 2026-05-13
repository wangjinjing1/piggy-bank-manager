package com.piggybank.manager.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("deposit_bill")
public class DepositBill {
    private Long id;
    private Long ownerUserId;
    private String depositorName;
    private String billType;
    private BigDecimal amount;
    private String bank;
    private LocalDate depositDate;
    private LocalDate dueDate;
    private String status;
    private String sourceType;
    private String auditStatus;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    public String getDepositorName() { return depositorName; }
    public void setDepositorName(String depositorName) { this.depositorName = depositorName; }
    public String getBillType() { return billType; }
    public void setBillType(String billType) { this.billType = billType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getBank() { return bank; }
    public void setBank(String bank) { this.bank = bank; }
    public LocalDate getDepositDate() { return depositDate; }
    public void setDepositDate(LocalDate depositDate) { this.depositDate = depositDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getAuditStatus() { return auditStatus; }
    public void setAuditStatus(String auditStatus) { this.auditStatus = auditStatus; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
