package com.piggybank.manager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String encryptionKey;
    private String tokenSecret;
    private String publicBaseUrl;
    private Admin admin = new Admin();
    private StartupMail startupMail = new StartupMail();
    private Security security = new Security();

    public String getEncryptionKey() { return encryptionKey; }
    public void setEncryptionKey(String encryptionKey) { this.encryptionKey = encryptionKey; }
    public String getTokenSecret() { return tokenSecret; }
    public void setTokenSecret(String tokenSecret) { this.tokenSecret = tokenSecret; }
    public String getPublicBaseUrl() { return publicBaseUrl; }
    public void setPublicBaseUrl(String publicBaseUrl) { this.publicBaseUrl = publicBaseUrl; }
    public Admin getAdmin() { return admin; }
    public void setAdmin(Admin admin) { this.admin = admin; }
    public StartupMail getStartupMail() { return startupMail; }
    public void setStartupMail(StartupMail startupMail) { this.startupMail = startupMail; }
    public Security getSecurity() { return security; }
    public void setSecurity(Security security) { this.security = security; }

    public static class Admin {
        private String username = "admin";
        private String password = "amdin";
        private String email = "admin@example.com";

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class StartupMail {
        private boolean enabled = false;
        private String to;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }
    }

    public static class Security {
        private int maxRequestsPerIpPerDay = 200;

        public int getMaxRequestsPerIpPerDay() { return maxRequestsPerIpPerDay; }
        public void setMaxRequestsPerIpPerDay(int maxRequestsPerIpPerDay) { this.maxRequestsPerIpPerDay = maxRequestsPerIpPerDay; }
    }
}
