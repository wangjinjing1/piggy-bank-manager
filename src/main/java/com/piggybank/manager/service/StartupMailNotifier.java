package com.piggybank.manager.service;

import com.piggybank.manager.config.AppProperties;
import com.piggybank.manager.util.DateFormatUtil;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Order(100)
public class StartupMailNotifier implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(StartupMailNotifier.class);
    private final AppProperties properties;
    private final MailService mailService;

    public StartupMailNotifier(AppProperties properties, MailService mailService) {
        this.properties = properties;
        this.mailService = mailService;
    }

    @Override
    public void run(ApplicationArguments args) {
        // 启动通知可关闭，避免开发或测试环境频繁发送邮件。
        if (!properties.getStartupMail().isEnabled()) {
            return;
        }
        String to = properties.getStartupMail().getTo();
        if (!StringUtils.hasText(to)) {
            to = properties.getAdmin().getEmail();
        }
        try {
            mailService.send(to, "借存管理应用启动成功",
                    "借存管理应用已启动。\n启动时间：" + DateFormatUtil.dateTime(LocalDateTime.now()));
        } catch (Exception ex) {
            log.warn("启动通知邮件发送失败，应用将继续启动: {}", ex.getMessage());
        }
    }
}
