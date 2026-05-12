package com.piggybank.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.piggybank.manager.config.AppProperties;
import com.piggybank.manager.domain.IpAccessStat;
import com.piggybank.manager.domain.IpBlacklist;
import com.piggybank.manager.mapper.IpAccessStatMapper;
import com.piggybank.manager.mapper.IpBlacklistMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class IpSecurityService {
    private final IpAccessStatMapper accessStatMapper;
    private final IpBlacklistMapper blacklistMapper;
    private final AppProperties properties;

    public IpSecurityService(IpAccessStatMapper accessStatMapper, IpBlacklistMapper blacklistMapper, AppProperties properties) {
        this.accessStatMapper = accessStatMapper;
        this.blacklistMapper = blacklistMapper;
        this.properties = properties;
    }

    public boolean isBlacklisted(String ip) {
        return blacklistMapper.selectCount(new LambdaQueryWrapper<IpBlacklist>().eq(IpBlacklist::getIp, ip)) > 0;
    }

    /*
     * 每个IP维护一个独立的24小时访问窗口。
     * 窗口过期后重置计数；窗口内超过配置阈值后写入黑名单表。
     */
    public synchronized void recordVisit(String ip) {
        LocalDateTime now = LocalDateTime.now();
        IpAccessStat stat = accessStatMapper.selectOne(new LambdaQueryWrapper<IpAccessStat>().eq(IpAccessStat::getIp, ip));
        if (stat == null) {
            stat = new IpAccessStat();
            stat.setIp(ip);
            stat.setRequestCount(1);
            stat.setWindowStartAt(now);
            stat.setCreatedAt(now);
            stat.setUpdatedAt(now);
            accessStatMapper.insert(stat);
            return;
        }

        if (stat.getWindowStartAt() == null || !now.isBefore(stat.getWindowStartAt().plusHours(24))) {
            stat.setRequestCount(1);
            stat.setWindowStartAt(now);
        } else {
            stat.setRequestCount((stat.getRequestCount() == null ? 0 : stat.getRequestCount()) + 1);
        }
        stat.setUpdatedAt(now);
        accessStatMapper.updateById(stat);

        if (stat.getRequestCount() > properties.getSecurity().getMaxRequestsPerIpPerDay()) {
            addBlacklist(ip, "24小时内访问次数超过限制", stat.getRequestCount());
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
            accessStatMapper.delete(new LambdaQueryWrapper<IpAccessStat>().eq(IpAccessStat::getIp, item.getIp()));
        }
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
