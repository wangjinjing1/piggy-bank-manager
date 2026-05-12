package com.piggybank.manager.config;

import com.piggybank.manager.service.UserService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;
    private final UserService userService;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate, UserService userService) {
        this.jdbcTemplate = jdbcTemplate;
        this.userService = userService;
    }

    @Override
    public void run(ApplicationArguments args) {
        /*
         * 轻量部署场景下不依赖额外迁移工具，应用启动时保证核心表存在。
         * 新增字段用 addColumnIfMissing，废弃字段用 dropColumnIfExists，避免旧库升级时报错。
         */
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS app_user (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  username_cipher TEXT NOT NULL,
                  password_cipher TEXT NOT NULL,
                  email VARCHAR(255),
                  role VARCHAR(32) NOT NULL,
                  enabled TINYINT(1) NOT NULL DEFAULT 1,
                  created_at DATETIME NOT NULL,
                  updated_at DATETIME NOT NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS borrow_bill (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  owner_user_id BIGINT NOT NULL,
                  borrower_name VARCHAR(120) NOT NULL,
                  status VARCHAR(32) NOT NULL,
                  phone VARCHAR(50) NOT NULL,
                  email VARCHAR(255) NOT NULL,
                  amount DECIMAL(18,2) NOT NULL,
                  paid_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
                  remaining_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
                  borrow_date DATE NOT NULL,
                  due_date DATE NOT NULL,
                  remark VARCHAR(100),
                  source_type VARCHAR(32) NOT NULL,
                  audit_status VARCHAR(32) NOT NULL,
                  audit_mail_status VARCHAR(32),
                  audit_mail_sent_at DATETIME,
                  audit_mail_error VARCHAR(500),
                  reminder_mail_status VARCHAR(32),
                  reminder_mail_sent_at DATETIME,
                  reminder_mail_error VARCHAR(500),
                  created_at DATETIME NOT NULL,
                  updated_at DATETIME NOT NULL,
                  INDEX idx_borrow_owner (owner_user_id),
                  INDEX idx_borrow_due (due_date)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        addColumnIfMissing("borrow_bill", "audit_mail_status", "VARCHAR(32)");
        addColumnIfMissing("borrow_bill", "audit_mail_sent_at", "DATETIME");
        addColumnIfMissing("borrow_bill", "audit_mail_error", "VARCHAR(500)");
        addColumnIfMissing("borrow_bill", "reminder_mail_status", "VARCHAR(32)");
        addColumnIfMissing("borrow_bill", "reminder_mail_sent_at", "DATETIME");
        addColumnIfMissing("borrow_bill", "reminder_mail_error", "VARCHAR(500)");
        addColumnIfMissing("borrow_bill", "paid_amount", "DECIMAL(18,2) NOT NULL DEFAULT 0");
        addColumnIfMissing("borrow_bill", "remaining_amount", "DECIMAL(18,2) NOT NULL DEFAULT 0");
        addColumnIfMissing("borrow_bill", "remark", "VARCHAR(100)");
        jdbcTemplate.execute("UPDATE borrow_bill SET remaining_amount = amount - paid_amount WHERE remaining_amount = 0 AND amount > paid_amount");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS borrow_repayment (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  borrow_bill_id BIGINT NOT NULL,
                  owner_user_id BIGINT NOT NULL,
                  amount DECIMAL(18,2) NOT NULL,
                  repayment_date DATE NOT NULL,
                  remark VARCHAR(100),
                  created_at DATETIME NOT NULL,
                  INDEX idx_repayment_bill (borrow_bill_id),
                  INDEX idx_repayment_owner (owner_user_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS borrow_link (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  owner_user_id BIGINT NOT NULL,
                  token VARCHAR(80) NOT NULL UNIQUE,
                  used TINYINT(1) NOT NULL DEFAULT 0,
                  submitted_bill_id BIGINT,
                  created_at DATETIME NOT NULL,
                  used_at DATETIME,
                  INDEX idx_link_owner (owner_user_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS deposit_bill (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  owner_user_id BIGINT NOT NULL,
                  depositor_name VARCHAR(120) NOT NULL DEFAULT '',
                  amount DECIMAL(18,2) NOT NULL,
                  bank VARCHAR(120) NOT NULL,
                  deposit_date DATE NOT NULL,
                  due_date DATE NOT NULL,
                  status VARCHAR(32) NOT NULL,
                  remark VARCHAR(100),
                  created_at DATETIME NOT NULL,
                  updated_at DATETIME NOT NULL,
                  INDEX idx_deposit_owner (owner_user_id),
                  INDEX idx_deposit_due (due_date)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        addColumnIfMissing("deposit_bill", "depositor_name", "VARCHAR(120) NOT NULL DEFAULT ''");
        addColumnIfMissing("deposit_bill", "remark", "VARCHAR(100)");
        dropColumnIfExists("deposit_bill", "withdrawn_amount");
        dropColumnIfExists("deposit_bill", "remaining_amount");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ip_blacklist (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  ip VARCHAR(64) NOT NULL UNIQUE,
                  reason VARCHAR(255) NOT NULL,
                  request_count INT NOT NULL,
                  blocked_date DATE NOT NULL,
                  created_at DATETIME NOT NULL,
                  updated_at DATETIME NOT NULL,
                  INDEX idx_ip_blacklist_date (blocked_date)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        // IP访问计数已迁移到Redis，旧表在升级启动时移除，数据库只保留黑名单结果。
        jdbcTemplate.execute("DROP TABLE IF EXISTS ip_access_stat");
        userService.ensureDefaultAdmin();
    }

    private void addColumnIfMissing(String tableName, String columnName, String definition) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """, Integer.class, tableName, columnName);
        if (count != null && count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }

    private void dropColumnIfExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """, Integer.class, tableName, columnName);
        if (count != null && count > 0) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP COLUMN " + columnName);
        }
    }
}
