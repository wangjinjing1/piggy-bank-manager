package com.piggybank.manager.service;

import com.piggybank.manager.domain.AppUser;
import com.piggybank.manager.domain.BorrowBill;
import com.piggybank.manager.domain.DepositBill;
import com.piggybank.manager.util.DateFormatUtil;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ReminderService {
    private final BorrowService borrowService;
    private final DepositService depositService;
    private final UserService userService;
    private final MailService mailService;

    public ReminderService(BorrowService borrowService, DepositService depositService, UserService userService, MailService mailService) {
        this.borrowService = borrowService;
        this.depositService = depositService;
        this.userService = userService;
        this.mailService = mailService;
    }

    @Scheduled(cron = "${app.reminder-cron}")
    public void sendDueReminders() {
        sendBorrowReminders();
        sendDepositReminders();
    }

    private void sendBorrowReminders() {
        Map<String, List<BorrowBill>> byEmail = borrowService.dueBills().stream()
                .collect(Collectors.groupingBy(BorrowBill::getEmail));
        byEmail.values().forEach(borrowService::sendReminderForBills);
    }

    private void sendDepositReminders() {
        for (DepositBill bill : depositService.dueBills()) {
            AppUser user = userService.getById(bill.getOwnerUserId());
            if (user != null) {
                mailService.send(user.getEmail(), "存款到期提醒",
                        "存款到期，请及时处理。"
                                + "\n银行：" + bill.getBank()
                                + "\n存款金额：" + bill.getAmount()
                                + "\n存款日期：" + DateFormatUtil.date(bill.getDepositDate())
                                + "\n到期日期：" + DateFormatUtil.date(bill.getDueDate()));
            }
        }
    }
}
