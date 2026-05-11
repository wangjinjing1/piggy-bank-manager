package com.piggybank.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.piggybank.manager.config.AppProperties;
import com.piggybank.manager.domain.BorrowBill;
import com.piggybank.manager.domain.BorrowLink;
import com.piggybank.manager.dto.BillDtos;
import com.piggybank.manager.mapper.BorrowBillMapper;
import com.piggybank.manager.mapper.BorrowLinkMapper;
import com.piggybank.manager.util.DateFormatUtil;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class BorrowService {
    private static final LocalDate FAR_FUTURE = LocalDate.of(9999, 12, 31);
    private final BorrowBillMapper billMapper;
    private final BorrowLinkMapper linkMapper;
    private final AppProperties properties;
    private final MailService mailService;
    private final SecureRandom random = new SecureRandom();

    public BorrowService(BorrowBillMapper billMapper, BorrowLinkMapper linkMapper, AppProperties properties, MailService mailService) {
        this.billMapper = billMapper;
        this.linkMapper = linkMapper;
        this.properties = properties;
        this.mailService = mailService;
    }

    public BorrowBill create(Long ownerId, BillDtos.BorrowRequest request, String sourceType, String auditStatus) {
        BorrowBill bill = new BorrowBill();
        bill.setOwnerUserId(ownerId);
        bill.setBorrowerName(request.getBorrowerName());
        bill.setStatus(normalizeStatus(request.getStatus()));
        bill.setPhone(request.getPhone());
        bill.setEmail(request.getEmail());
        bill.setAmount(request.getAmount());
        bill.setBorrowDate(request.getBorrowDate() == null ? LocalDate.now() : request.getBorrowDate());
        bill.setDueDate(request.getDueDate() == null ? FAR_FUTURE : request.getDueDate());
        bill.setSourceType(sourceType);
        bill.setAuditStatus(auditStatus);
        bill.setAuditMailStatus("NOT_SENT");
        bill.setReminderMailStatus("NOT_SENT");
        bill.setCreatedAt(LocalDateTime.now());
        bill.setUpdatedAt(LocalDateTime.now());
        billMapper.insert(bill);
        return bill;
    }

    public List<BorrowBill> list(Long ownerId) {
        return billMapper.selectList(new LambdaQueryWrapper<BorrowBill>()
                .eq(BorrowBill::getOwnerUserId, ownerId)
                .orderByDesc(BorrowBill::getCreatedAt));
    }

    public List<BorrowBill> list(Long ownerId, BillDtos.BorrowListQuery query) {
        return billMapper.selectList(new LambdaQueryWrapper<BorrowBill>()
                .eq(BorrowBill::getOwnerUserId, ownerId)
                .like(StringUtils.hasText(query.getBorrowerName()), BorrowBill::getBorrowerName, query.getBorrowerName())
                .like(StringUtils.hasText(query.getPhone()), BorrowBill::getPhone, query.getPhone())
                .like(StringUtils.hasText(query.getEmail()), BorrowBill::getEmail, query.getEmail())
                .ge(query.getBorrowStartDate() != null, BorrowBill::getBorrowDate, query.getBorrowStartDate())
                .le(query.getBorrowEndDate() != null, BorrowBill::getBorrowDate, query.getBorrowEndDate())
                .ge(query.getDueStartDate() != null, BorrowBill::getDueDate, query.getDueStartDate())
                .le(query.getDueEndDate() != null, BorrowBill::getDueDate, query.getDueEndDate())
                .orderByDesc(BorrowBill::getCreatedAt));
    }

    public BorrowBill approve(Long ownerId, Long id) {
        BorrowBill bill = getOwned(ownerId, id);
        bill.setAuditStatus("APPROVED");
        bill.setUpdatedAt(LocalDateTime.now());
        billMapper.updateById(bill);
        sendAuditApprovedMail(bill);
        return billMapper.selectById(id);
    }

    public BorrowBill updateStatus(Long ownerId, Long id, String status) {
        BorrowBill bill = getOwned(ownerId, id);
        bill.setStatus(normalizeStatus(status));
        bill.setUpdatedAt(LocalDateTime.now());
        billMapper.updateById(bill);
        return bill;
    }

    public BorrowBill update(Long ownerId, Long id, BillDtos.BorrowRequest request) {
        BorrowBill bill = getOwned(ownerId, id);
        bill.setBorrowerName(request.getBorrowerName());
        bill.setStatus(normalizeStatus(request.getStatus()));
        bill.setPhone(request.getPhone());
        bill.setEmail(request.getEmail());
        bill.setAmount(request.getAmount());
        bill.setBorrowDate(request.getBorrowDate() == null ? LocalDate.now() : request.getBorrowDate());
        bill.setDueDate(request.getDueDate() == null ? FAR_FUTURE : request.getDueDate());
        bill.setUpdatedAt(LocalDateTime.now());
        billMapper.updateById(bill);
        return bill;
    }

    public BorrowBill updateOwner(Long id, Long ownerUserId) {
        if (ownerUserId == null) {
            throw new IllegalArgumentException("所属用户不能为空");
        }
        BorrowBill bill = billMapper.selectById(id);
        if (bill == null) {
            throw new IllegalArgumentException("记录不存在");
        }
        bill.setOwnerUserId(ownerUserId);
        bill.setUpdatedAt(LocalDateTime.now());
        billMapper.updateById(bill);
        return bill;
    }

    public void delete(Long ownerId, Long id) {
        BorrowBill bill = getOwned(ownerId, id);
        billMapper.deleteById(bill.getId());
    }

    public String createLink(Long ownerId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        BorrowLink link = new BorrowLink();
        link.setOwnerUserId(ownerId);
        link.setToken(token);
        link.setUsed(false);
        link.setCreatedAt(LocalDateTime.now());
        linkMapper.insert(link);
        return properties.getPublicBaseUrl() + "/public-borrow.html?token=" + token;
    }

    @Transactional
    public BorrowBill submitPublic(String token, BillDtos.BorrowRequest request) {
        // 匿名链接提交和标记已使用必须在同一事务里完成，防止链接被重复使用。
        BorrowLink link = linkMapper.selectOne(new LambdaQueryWrapper<BorrowLink>().eq(BorrowLink::getToken, token));
        if (link == null || Boolean.TRUE.equals(link.getUsed())) {
            throw new IllegalStateException("链接不存在或已被使用");
        }
        request.setStatus("NORMAL");
        BorrowBill bill = create(link.getOwnerUserId(), request, "LINK", "PENDING");
        link.setUsed(true);
        link.setSubmittedBillId(bill.getId());
        link.setUsedAt(LocalDateTime.now());
        linkMapper.updateById(link);
        return bill;
    }

    public List<BorrowBill> report(Long ownerId, LocalDate start, LocalDate end, String name) {
        return billMapper.selectList(new LambdaQueryWrapper<BorrowBill>()
                .eq(BorrowBill::getOwnerUserId, ownerId)
                .eq(BorrowBill::getStatus, "NORMAL")
                .eq(BorrowBill::getAuditStatus, "APPROVED")
                .ge(start != null, BorrowBill::getBorrowDate, start)
                .le(BorrowBill::getBorrowDate, end == null ? FAR_FUTURE : end)
                .like(StringUtils.hasText(name), BorrowBill::getBorrowerName, name)
                .orderByDesc(BorrowBill::getBorrowDate));
    }

    public List<BorrowBill> dueBills() {
        return billMapper.selectList(new LambdaQueryWrapper<BorrowBill>()
                .eq(BorrowBill::getStatus, "NORMAL")
                .eq(BorrowBill::getAuditStatus, "APPROVED")
                .le(BorrowBill::getDueDate, LocalDate.now()));
    }

    public List<Map<String, Object>> overdueGroups(Long ownerId) {
        Map<String, List<BorrowBill>> grouped = overdueBills(ownerId).stream()
                .collect(Collectors.groupingBy(BorrowBill::getEmail, LinkedHashMap::new, Collectors.toList()));
        List<Map<String, Object>> result = new ArrayList<>();
        grouped.forEach((email, bills) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("email", email);
            item.put("borrowerName", bills.get(0).getBorrowerName());
            item.put("phone", bills.get(0).getPhone());
            item.put("total", sum(bills));
            item.put("count", bills.size());
            item.put("lastReminderMailStatus", bills.get(0).getReminderMailStatus());
            item.put("lastReminderMailSentAt", bills.get(0).getReminderMailSentAt());
            item.put("lastReminderMailError", bills.get(0).getReminderMailError());
            item.put("items", bills);
            result.add(item);
        });
        return result;
    }

    public void sendOverdueReminder(Long ownerId, String email) {
        List<BorrowBill> bills = overdueBills(ownerId).stream()
                .filter(bill -> email.equals(bill.getEmail()))
                .toList();
        if (bills.isEmpty()) {
            throw new IllegalArgumentException("没有找到该邮箱的逾期借条");
        }
        sendReminderForBills(bills);
    }

    public void sendReminderForBills(List<BorrowBill> bills) {
        if (bills.isEmpty()) {
            return;
        }
        String email = bills.get(0).getEmail();
        BigDecimal total = sum(bills);
        String detail = bills.stream()
                .map(b -> "借款人：" + b.getBorrowerName()
                        + "，借款日期：" + DateFormatUtil.date(b.getBorrowDate())
                        + "，金额：" + b.getAmount()
                        + "，还款日期：" + DateFormatUtil.date(b.getDueDate()))
                .collect(Collectors.joining("\n"));
        try {
            mailService.send(email, "借款逾期提醒",
                    "您有借款已逾期，请及时处理。\n总金额：" + total + "\n明细：\n" + detail);
            updateReminderStatus(bills, "SENT", null);
        } catch (Exception ex) {
            updateReminderStatus(bills, "FAILED", ex.getMessage());
            throw ex;
        }
    }

    public BigDecimal sum(List<BorrowBill> bills) {
        return bills.stream().map(BorrowBill::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void sendAuditApprovedMail(BorrowBill bill) {
        try {
            mailService.send(bill.getEmail(), "借款审核已通过",
                    "您的借款信息已审核通过。"
                            + "\n借款日期：" + DateFormatUtil.date(bill.getBorrowDate())
                            + "\n借款金额：" + bill.getAmount()
                            + "\n还款日期：" + DateFormatUtil.date(bill.getDueDate()));
            bill.setAuditMailStatus("SENT");
            bill.setAuditMailSentAt(LocalDateTime.now());
            bill.setAuditMailError(null);
        } catch (Exception ex) {
            bill.setAuditMailStatus("FAILED");
            bill.setAuditMailError(ex.getMessage());
        }
        bill.setUpdatedAt(LocalDateTime.now());
        billMapper.updateById(bill);
    }

    private void updateReminderStatus(List<BorrowBill> bills, String status, String error) {
        for (BorrowBill bill : bills) {
            bill.setReminderMailStatus(status);
            bill.setReminderMailSentAt("SENT".equals(status) ? LocalDateTime.now() : bill.getReminderMailSentAt());
            bill.setReminderMailError(error);
            bill.setUpdatedAt(LocalDateTime.now());
            billMapper.updateById(bill);
        }
    }

    private List<BorrowBill> overdueBills(Long ownerId) {
        return billMapper.selectList(new LambdaQueryWrapper<BorrowBill>()
                .eq(BorrowBill::getOwnerUserId, ownerId)
                .eq(BorrowBill::getStatus, "NORMAL")
                .eq(BorrowBill::getAuditStatus, "APPROVED")
                .lt(BorrowBill::getDueDate, LocalDate.now())
                .orderByAsc(BorrowBill::getDueDate));
    }

    private BorrowBill getOwned(Long ownerId, Long id) {
        BorrowBill bill = billMapper.selectById(id);
        if (bill == null || !ownerId.equals(bill.getOwnerUserId())) {
            throw new IllegalArgumentException("记录不存在");
        }
        return bill;
    }

    private String normalizeStatus(String status) {
        return "VOID".equalsIgnoreCase(status) ? "VOID" : "NORMAL";
    }
}
