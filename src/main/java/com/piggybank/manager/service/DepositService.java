package com.piggybank.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.piggybank.manager.domain.DepositBill;
import com.piggybank.manager.domain.DepositWithdrawal;
import com.piggybank.manager.dto.BillDtos;
import com.piggybank.manager.mapper.DepositBillMapper;
import com.piggybank.manager.mapper.DepositWithdrawalMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DepositService {
    private static final LocalDate FAR_FUTURE = LocalDate.of(9999, 12, 31);
    private final DepositBillMapper depositMapper;
    private final DepositWithdrawalMapper withdrawalMapper;

    public DepositService(DepositBillMapper depositMapper, DepositWithdrawalMapper withdrawalMapper) {
        this.depositMapper = depositMapper;
        this.withdrawalMapper = withdrawalMapper;
    }

    public DepositBill create(Long ownerId, BillDtos.DepositRequest request) {
        DepositBill bill = new DepositBill();
        bill.setOwnerUserId(ownerId);
        bill.setDepositorName(request.getDepositorName());
        bill.setAmount(request.getAmount());
        bill.setWithdrawnAmount(BigDecimal.ZERO);
        bill.setRemainingAmount(request.getAmount());
        bill.setBank(request.getBank());
        bill.setDepositDate(request.getDepositDate() == null ? LocalDate.now() : request.getDepositDate());
        bill.setDueDate(request.getDueDate());
        bill.setStatus(normalizeStatus(request.getStatus()));
        bill.setRemark(request.getRemark());
        bill.setCreatedAt(LocalDateTime.now());
        bill.setUpdatedAt(LocalDateTime.now());
        depositMapper.insert(bill);
        return bill;
    }

    public List<DepositBill> list(Long ownerId) {
        return depositMapper.selectList(new LambdaQueryWrapper<DepositBill>()
                .eq(DepositBill::getOwnerUserId, ownerId)
                .orderByDesc(DepositBill::getCreatedAt));
    }

    public Map<String, Object> list(Long ownerId, BillDtos.DepositListQuery query) {
        List<DepositBill> bills = depositMapper.selectList(new LambdaQueryWrapper<DepositBill>()
                .eq(DepositBill::getOwnerUserId, ownerId)
                .like(StringUtils.hasText(query.getDepositorName()), DepositBill::getDepositorName, query.getDepositorName())
                .like(StringUtils.hasText(query.getBank()), DepositBill::getBank, query.getBank())
                .ge(query.getDepositStartDate() != null, DepositBill::getDepositDate, query.getDepositStartDate())
                .le(query.getDepositEndDate() != null, DepositBill::getDepositDate, query.getDepositEndDate())
                .ge(query.getDueStartDate() != null, DepositBill::getDueDate, query.getDueStartDate())
                .le(query.getDueEndDate() != null, DepositBill::getDueDate, query.getDueEndDate())
                .orderByDesc(DepositBill::getCreatedAt));
        return page(bills, query.getPage(), query.getSize());
    }

    public DepositBill updateStatus(Long ownerId, Long id, String status) {
        DepositBill bill = getOwned(ownerId, id);
        bill.setStatus(normalizeStatus(status));
        bill.setUpdatedAt(LocalDateTime.now());
        depositMapper.updateById(bill);
        return bill;
    }

    public DepositBill update(Long ownerId, Long id, BillDtos.DepositRequest request) {
        DepositBill bill = getOwned(ownerId, id);
        bill.setDepositorName(request.getDepositorName());
        bill.setAmount(request.getAmount());
        BigDecimal withdrawn = bill.getWithdrawnAmount() == null ? BigDecimal.ZERO : bill.getWithdrawnAmount();
        if (request.getAmount().compareTo(withdrawn) < 0) {
            throw new IllegalArgumentException("存款金额不能小于已取金额");
        }
        bill.setRemainingAmount(request.getAmount().subtract(withdrawn));
        bill.setBank(request.getBank());
        bill.setDepositDate(request.getDepositDate() == null ? LocalDate.now() : request.getDepositDate());
        bill.setDueDate(request.getDueDate());
        bill.setStatus(normalizeStatus(request.getStatus()));
        bill.setRemark(request.getRemark());
        bill.setUpdatedAt(LocalDateTime.now());
        depositMapper.updateById(bill);
        return bill;
    }

    @Transactional
    public DepositWithdrawal withdraw(Long ownerId, Long id, BillDtos.WithdrawalRequest request) {
        DepositBill bill = getOwned(ownerId, id);
        BigDecimal remaining = bill.getRemainingAmount() == null ? bill.getAmount() : bill.getRemainingAmount();
        if (request.getAmount().compareTo(remaining) > 0) {
            throw new IllegalArgumentException("存款余额不足，无法取款");
        }
        DepositWithdrawal withdrawal = new DepositWithdrawal();
        withdrawal.setDepositBillId(id);
        withdrawal.setOwnerUserId(ownerId);
        withdrawal.setDepositorName(request.getDepositorName());
        withdrawal.setAmount(request.getAmount());
        withdrawal.setWithdrawalDate(request.getWithdrawalDate() == null ? LocalDate.now() : request.getWithdrawalDate());
        withdrawal.setRemark(request.getRemark());
        withdrawal.setCreatedAt(LocalDateTime.now());
        withdrawalMapper.insert(withdrawal);

        BigDecimal withdrawn = bill.getWithdrawnAmount() == null ? BigDecimal.ZERO : bill.getWithdrawnAmount();
        bill.setWithdrawnAmount(withdrawn.add(request.getAmount()));
        bill.setRemainingAmount(remaining.subtract(request.getAmount()));
        bill.setUpdatedAt(LocalDateTime.now());
        depositMapper.updateById(bill);
        return withdrawal;
    }

    public Map<String, Object> detail(Long ownerId, Long id, int page, int size) {
        DepositBill bill = getOwned(ownerId, id);
        List<DepositWithdrawal> records = withdrawalMapper.selectList(new LambdaQueryWrapper<DepositWithdrawal>()
                .eq(DepositWithdrawal::getDepositBillId, id)
                .orderByDesc(DepositWithdrawal::getWithdrawalDate)
                .orderByDesc(DepositWithdrawal::getCreatedAt));
        return Map.of("bill", bill, "withdrawals", page(records, page, size));
    }

    public DepositBill updateOwner(Long id, Long ownerUserId) {
        if (ownerUserId == null) {
            throw new IllegalArgumentException("所属用户不能为空");
        }
        DepositBill bill = depositMapper.selectById(id);
        if (bill == null) {
            throw new IllegalArgumentException("记录不存在");
        }
        bill.setOwnerUserId(ownerUserId);
        bill.setUpdatedAt(LocalDateTime.now());
        depositMapper.updateById(bill);
        return bill;
    }

    public void delete(Long ownerId, Long id) {
        DepositBill bill = getOwned(ownerId, id);
        depositMapper.deleteById(bill.getId());
    }

    public List<DepositBill> report(Long ownerId, LocalDate start, LocalDate end, String name) {
        return depositMapper.selectList(new LambdaQueryWrapper<DepositBill>()
                .eq(DepositBill::getOwnerUserId, ownerId)
                .eq(DepositBill::getStatus, "NORMAL")
                .ge(start != null, DepositBill::getDepositDate, start)
                .le(DepositBill::getDepositDate, end == null ? FAR_FUTURE : end)
                .like(StringUtils.hasText(name), DepositBill::getDepositorName, name)
                .orderByDesc(DepositBill::getDepositDate));
    }

    public BigDecimal sum(List<DepositBill> bills) {
        return bills.stream().map(b -> b.getRemainingAmount() == null ? b.getAmount() : b.getRemainingAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<String, Object> reportGroups(Long ownerId, LocalDate start, LocalDate end, String name, int page, int size) {
        List<Map<String, Object>> groups = report(ownerId, start, end, name).stream()
                .collect(Collectors.groupingBy(DepositBill::getDepositorName, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(entry -> {
                    List<DepositBill> bills = entry.getValue();
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("name", entry.getKey());
                    data.put("count", bills.size());
                    data.put("total", sum(bills));
                    data.put("items", bills);
                    return data;
                })
                .toList();
        BigDecimal total = groups.stream()
                .map(item -> (BigDecimal) item.get("total"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Map.of("total", total, "groups", page(groups, page, size));
    }

    public List<DepositBill> dueBills() {
        return depositMapper.selectList(new LambdaQueryWrapper<DepositBill>()
                .eq(DepositBill::getStatus, "NORMAL")
                .le(DepositBill::getDueDate, LocalDate.now()));
    }

    private DepositBill getOwned(Long ownerId, Long id) {
        DepositBill bill = depositMapper.selectById(id);
        if (bill == null || !ownerId.equals(bill.getOwnerUserId())) {
            throw new IllegalArgumentException("记录不存在");
        }
        return bill;
    }

    private String normalizeStatus(String status) {
        return "VOID".equalsIgnoreCase(status) ? "VOID" : "NORMAL";
    }

    private <T> Map<String, Object> page(List<T> items, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int from = Math.min((safePage - 1) * safeSize, items.size());
        int to = Math.min(from + safeSize, items.size());
        return Map.of("page", safePage, "size", safeSize, "total", items.size(), "items", items.subList(from, to));
    }
}
