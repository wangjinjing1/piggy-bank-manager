package com.piggybank.manager.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("ip_access_stat")
public class IpAccessStat {
    private Long id;
    private String ip;
    private Integer requestCount;
    private LocalDateTime windowStartAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public Integer getRequestCount() { return requestCount; }
    public void setRequestCount(Integer requestCount) { this.requestCount = requestCount; }
    public LocalDateTime getWindowStartAt() { return windowStartAt; }
    public void setWindowStartAt(LocalDateTime windowStartAt) { this.windowStartAt = windowStartAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
