# 借存管理应用

单体 Spring Boot 应用，后端使用 Java + MySQL + MyBatis Plus，前端静态 Vue 页面由同一个 Spring Boot 服务托管。

## 功能

- 默认管理员：admin / amdin
- 管理员新增和编辑用户，包括超级管理员用户名、密码、邮箱
- 用户创建借账单、存账单
- 借款匿名填写链接，每个链接只能提交一次，提交后待审核
- 审核通过后邮件通知借款人
- 借款到期、存款到期定时邮件提醒
- 启动通知邮件，可配置是否发送
- IP 单日访问超限自动加入黑名单，管理员可查看和移除
- 按类型、日期范围、姓名查询总额和明细
- CSV 导出
- 启动时自动创建数据表
- 用户名和密码使用配置密钥 AES-GCM 加密存储
- Docker Compose 部署

## 配置

`src/main/resources/application.yaml` 保持在 Spring Boot 默认位置，但只保留环境变量占位符，不写真实密码、邮箱授权码等敏感信息。

Dockerfile 同级目录的 `.env` 保存敏感配置，并通过 `docker-compose.yml` 的 `env_file` 注入容器。`.env` 已加入 `.gitignore` 和 `.dockerignore`，不会提交到 Git，也不会进入 Docker 构建上下文。

## 本地运行

1. 准备 MySQL，并创建或允许自动创建 `piggy_bank` 数据库。
2. 本地直接运行时，通过系统环境变量提供数据库、邮件、密钥配置；Docker 运行时修改 `.env`。
3. 启动：

```bash
mvn spring-boot:run
```

访问 `http://localhost:8080`。

## Docker 部署

```bash
docker compose up --build
```

如果使用 Compose 内置 MySQL，`.env` 中的数据源地址可以使用：

```yaml
spring:
  datasource:
    url: jdbc:mysql://mysql:3306/piggy_bank?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&createDatabaseIfNotExist=true
    username: root
    password: root
```

生产环境请在 `.env` 中修改：

- `app.encryption-key`
- `app.token-secret`
- `spring.mail.*`
- `app.public-base-url`
- MySQL 密码

## 邮件提醒

默认每天 09:00 扫描到期借款和存款，可在 `config/application.yaml` 中修改：

```yaml
app:
  reminder-cron: "0 0 9 * * ?"
```

## 启动通知邮件

```yaml
app:
  startup-mail:
    enabled: true
    to: your-email@example.com
```

如果 `to` 留空，会发送到默认管理员邮箱 `app.admin.email`。

## IP 黑名单

```yaml
app:
  security:
    max-requests-per-ip-per-day: 200
```

超过阈值后会自动写入 `ip_blacklist` 表，管理员可在页面中查看和移除。
