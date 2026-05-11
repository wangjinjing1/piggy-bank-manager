package com.piggybank.manager.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("deposit_withdrawal")
public class DepositWithdrawal {
    private Long id;
    private Long depositBillId;
    private Long ownerUserId;
    private String depositorName;
    private BigDecimal amount;
    private LocalDate withdrawalDate;
    private String remark;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDepositBillId() { return depositBillId; }
    public void setDepositBillId(Long depositBillId) { this.depositBillId = depositBillId; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    public String getDepositorName() { return depositorName; }
    public void setDepositorName(String depositorName) { this.depositorName = depositorName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getWithdrawalDate() { return withdrawalDate; }
    public void setWithdrawalDate(LocalDate withdrawalDate) { this.withdrawalDate = withdrawalDate; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
