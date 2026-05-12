package com.piggybank.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.piggybank.manager.config.AppProperties;
import com.piggybank.manager.domain.IpBlacklist;
import com.piggybank.manager.mapper.IpBlacklistMapper;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class IpSecurityService {
    private static final String ACCESS_COUNTER_PREFIX = "security:ip:access:";
    private static final Duration ACCESS_WINDOW = Duration.ofHours(24);
    private static final DefaultRedisScript<Long> RECORD_VISIT_SCRIPT = new DefaultRedisScript<>("""
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local windowMillis = tonumber(ARGV[2])
            local ttlSeconds = tonumber(ARGV[3])
            local startedAt = tonumber(redis.call('HGET', key, 'startedAt'))
            local count = tonumber(redis.call('HGET', key, 'count')) or 0

            if startedAt == nil or now - startedAt >= windowMillis then
              startedAt = now
              count = 1
            else
              count = count + 1
            end

            redis.call('HSET', key, 'startedAt', startedAt, 'count', count)
            redis.call('EXPIRE', key, ttlSeconds)
            return count
            """, Long.class);
    private final StringRedisTemplate redisTemplate;
    private final IpBlacklistMapper blacklistMapper;
    private final AppProperties properties;

    public IpSecurityService(StringRedisTemplate redisTemplate, IpBlacklistMapper blacklistMapper, AppProperties properties) {
        this.redisTemplate = redisTemplate;
        this.blacklistMapper = blacklistMapper;
        this.properties = properties;
    }

    public boolean isBlacklisted(String ip) {
        return blacklistMapper.selectCount(new LambdaQueryWrapper<IpBlacklist>().eq(IpBlacklist::getIp, ip)) > 0;
    }

    /*
     * startedAt负责固定的24小时统计窗口，TTL只负责“24小时无访问自动释放Redis键”。
     * 这样连续访问不会无限延长统计窗口，但长时间不访问的IP也不会长期占内存。
     * Lua脚本让“判断窗口、累加次数、续期TTL”保持原子性。
     */
    public void recordVisit(String ip) {
        String key = accessCounterKey(ip);
        Long count = redisTemplate.execute(
                RECORD_VISIT_SCRIPT,
                Collections.singletonList(key),
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(ACCESS_WINDOW.toMillis()),
                String.valueOf(ACCESS_WINDOW.toSeconds()));
        if (count != null && count > properties.getSecurity().getMaxRequestsPerIpPerDay()) {
            addBlacklist(ip, "24小时内访问次数超过限制", count.intValue());
        }
    }

    public List<IpBlacklist> listBlacklist() {
        return blacklistMapper.selectList(new LambdaQueryWrapper<IpBlacklist>().orderByDesc(IpBlacklist::getCreatedAt));
    }

    public void remove(Long id) {
        // 移出黑名单时同步清掉计数，避免用户刚解除又被旧窗口次数再次拉黑。
        IpBlacklist item = blacklistMapper.selectById(id);
        blacklistMapper.deleteById(id);
        if (item != null) {
            redisTemplate.delete(accessCounterKey(item.getIp()));
        }
    }

    private String accessCounterKey(String ip) {
        return ACCESS_COUNTER_PREFIX + ip;
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
