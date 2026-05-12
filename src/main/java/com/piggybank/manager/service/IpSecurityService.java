package com.piggybank.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.piggybank.manager.config.AppProperties;
import com.piggybank.manager.domain.IpBlacklist;
import com.piggybank.manager.mapper.IpBlacklistMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class IpSecurityService {
    private static final String ACCESS_COUNTER_PREFIX = "security:ip:access:";
    private static final Duration ACCESS_WINDOW = Duration.ofHours(24);
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
     * 每个IP维护一个独立的Redis计数键。
     * 每次访问都会把TTL续到24小时；连续24小时无访问时键自动过期，减少Redis占用。
     * 计数超过配置阈值后写入MySQL黑名单表，黑名单仍然只允许管理员手动解除。
     */
    public void recordVisit(String ip) {
        String key = accessCounterKey(ip);
        Long count = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, ACCESS_WINDOW);
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
