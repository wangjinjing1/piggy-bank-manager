package com.piggybank.manager.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("borrow_bill")
public class BorrowBill {
    private Long id;
    private Long ownerUserId;
    private String borrowerName;
    private String status;
    private String phone;
    private String email;
    private BigDecimal amount;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private String sourceType;
    private String auditStatus;
    private String auditMailStatus;
    private LocalDateTime auditMailSentAt;
    private String auditMailError;
    private String reminderMailStatus;
    private LocalDateTime reminderMailSentAt;
    private String reminderMailError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    public String getBorrowerName() { return borrowerName; }
    public void setBorrowerName(String borrowerName) { this.borrowerName = borrowerName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getBorrowDate() { return borrowDate; }
    public void setBorrowDate(LocalDate borrowDate) { this.borrowDate = borrowDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getAuditStatus() { return auditStatus; }
    public void setAuditStatus(String auditStatus) { this.auditStatus = auditStatus; }
    public String getAuditMailStatus() { return auditMailStatus; }
    public void setAuditMailStatus(String auditMailStatus) { this.auditMailStatus = auditMailStatus; }
    public LocalDateTime getAuditMailSentAt() { return auditMailSentAt; }
    public void setAuditMailSentAt(LocalDateTime auditMailSentAt) { this.auditMailSentAt = auditMailSentAt; }
    public String getAuditMailError() { return auditMailError; }
    public void setAuditMailError(String auditMailError) { this.auditMailError = auditMailError; }
    public String getReminderMailStatus() { return reminderMailStatus; }
    public void setReminderMailStatus(String reminderMailStatus) { this.reminderMailStatus = reminderMailStatus; }
    public LocalDateTime getReminderMailSentAt() { return reminderMailSentAt; }
    public void setReminderMailSentAt(LocalDateTime reminderMailSentAt) { this.reminderMailSentAt = reminderMailSentAt; }
    public String getReminderMailError() { return reminderMailError; }
    public void setReminderMailError(String reminderMailError) { this.reminderMailError = reminderMailError; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
