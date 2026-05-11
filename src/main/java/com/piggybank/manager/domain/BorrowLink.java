package com.piggybank.manager.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("borrow_link")
public class BorrowLink {
    private Long id;
    private Long ownerUserId;
    private String token;
    private Boolean used;
    private Long submittedBillId;
    private LocalDateTime createdAt;
    private LocalDateTime usedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Boolean getUsed() { return used; }
    public void setUsed(Boolean used) { this.used = used; }
    public Long getSubmittedBillId() { return submittedBillId; }
    public void setSubmittedBillId(Long submittedBillId) { this.submittedBillId = submittedBillId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }
}
