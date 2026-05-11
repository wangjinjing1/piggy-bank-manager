package com.piggybank.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.piggybank.manager.domain.DepositBill;
import com.piggybank.manager.dto.BillDtos;
import com.piggybank.manager.mapper.DepositBillMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DepositService {
    private static final LocalDate FAR_FUTURE = LocalDate.of(9999, 12, 31);
    private final DepositBillMapper depositMapper;

    public DepositService(DepositBillMapper depositMapper) {
        this.depositMapper = depositMapper;
    }

    public DepositBill create(Long ownerId, BillDtos.DepositRequest request) {
        DepositBill bill = new DepositBill();
        bill.setOwnerUserId(ownerId);
        bill.setAmount(request.getAmount());
        bill.setBank(request.getBank());
        bill.setDepositDate(request.getDepositDate() == null ? LocalDate.now() : request.getDepositDate());
        bill.setDueDate(request.getDueDate());
        bill.setStatus(normalizeStatus(request.getStatus()));
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

    public List<DepositBill> list(Long ownerId, BillDtos.DepositListQuery query) {
        return depositMapper.selectList(new LambdaQueryWrapper<DepositBill>()
                .eq(DepositBill::getOwnerUserId, ownerId)
                .like(StringUtils.hasText(query.getBank()), DepositBill::getBank, query.getBank())
                .ge(query.getDepositStartDate() != null, DepositBill::getDepositDate, query.getDepositStartDate())
                .le(query.getDepositEndDate() != null, DepositBill::getDepositDate, query.getDepositEndDate())
                .ge(query.getDueStartDate() != null, DepositBill::getDueDate, query.getDueStartDate())
                .le(query.getDueEndDate() != null, DepositBill::getDueDate, query.getDueEndDate())
                .orderByDesc(DepositBill::getCreatedAt));
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
        bill.setAmount(request.getAmount());
        bill.setBank(request.getBank());
        bill.setDepositDate(request.getDepositDate() == null ? LocalDate.now() : request.getDepositDate());
        bill.setDueDate(request.getDueDate());
        bill.setStatus(normalizeStatus(request.getStatus()));
        bill.setUpdatedAt(LocalDateTime.now());
        depositMapper.updateById(bill);
        return bill;
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

    public List<DepositBill> report(Long ownerId, LocalDate start, LocalDate end) {
        return depositMapper.selectList(new LambdaQueryWrapper<DepositBill>()
                .eq(DepositBill::getOwnerUserId, ownerId)
                .eq(DepositBill::getStatus, "NORMAL")
                .ge(start != null, DepositBill::getDepositDate, start)
                .le(DepositBill::getDepositDate, end == null ? FAR_FUTURE : end)
                .orderByDesc(DepositBill::getDepositDate));
    }

    public BigDecimal sum(List<DepositBill> bills) {
        return bills.stream().map(DepositBill::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
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
}
