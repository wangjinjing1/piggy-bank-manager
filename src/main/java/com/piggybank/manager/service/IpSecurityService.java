package com.piggybank.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.piggybank.manager.config.AppProperties;
import com.piggybank.manager.domain.IpBlacklist;
import com.piggybank.manager.mapper.IpBlacklistMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class IpSecurityService {
    private final IpBlacklistMapper blacklistMapper;
    private final AppProperties properties;
    private final ConcurrentHashMap<String, AtomicInteger> dailyCounters = new ConcurrentHashMap<>();

    public IpSecurityService(IpBlacklistMapper blacklistMapper, AppProperties properties) {
        this.blacklistMapper = blacklistMapper;
        this.properties = properties;
    }

    public boolean isBlacklisted(String ip) {
        return blacklistMapper.selectCount(new LambdaQueryWrapper<IpBlacklist>().eq(IpBlacklist::getIp, ip)) > 0;
    }

    public void recordVisit(String ip) {
        String key = LocalDate.now() + ":" + ip;
        int count = dailyCounters.computeIfAbsent(key, ignored -> new AtomicInteger()).incrementAndGet();
        if (count > properties.getSecurity().getMaxRequestsPerIpPerDay()) {
            addBlacklist(ip, "单日访问次数超过限制", count);
        }
    }

    public List<IpBlacklist> listBlacklist() {
        return blacklistMapper.selectList(new LambdaQueryWrapper<IpBlacklist>().orderByDesc(IpBlacklist::getCreatedAt));
    }

    public void remove(Long id) {
        blacklistMapper.deleteById(id);
    }

    private void addBlacklist(String ip, String reason, int count) {
        IpBlacklist existing = blacklistMapper.selectOne(new LambdaQueryWrapper<IpBlacklist>().eq(IpBlacklist::getIp, ip));
        if (existing != null) {
            existing.setRequestCount(Math.max(existing.getRequestCount(), count));
            existing.setUpdatedAt(LocalDateTime.now());
            blacklistMapper.updateById(existing);
            return;
        }
        IpBlacklist item = new IpBlacklist();
        item.setIp(ip);
        item.setReason(reason);
        item.setRequestCount(count);
        item.setBlockedDate(LocalDate.now());
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        blacklistMapper.insert(item);
    }
}
