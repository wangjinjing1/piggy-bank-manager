package com.piggybank.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.piggybank.manager.config.AppProperties;
import com.piggybank.manager.domain.AppUser;
import com.piggybank.manager.dto.AuthDtos;
import com.piggybank.manager.dto.UserPrincipal;
import com.piggybank.manager.mapper.AppUserMapper;
import com.piggybank.manager.util.CryptoUtil;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final AppUserMapper userMapper;
    private final CryptoUtil cryptoUtil;
    private final AppProperties properties;

    public UserService(AppUserMapper userMapper, CryptoUtil cryptoUtil, AppProperties properties) {
        this.userMapper = userMapper;
        this.cryptoUtil = cryptoUtil;
        this.properties = properties;
    }

    public void ensureDefaultAdmin() {
        // If the configured administrator already exists, keep its current password unchanged.
        String adminUsername = properties.getAdmin().getUsername();
        AppUser configuredAdmin = findByUsername(adminUsername);
        if (configuredAdmin != null) {
            configuredAdmin.setEmail(properties.getAdmin().getEmail());
            configuredAdmin.setRole("ADMIN");
            configuredAdmin.setEnabled(true);
            configuredAdmin.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(configuredAdmin);
            log.info("已确认管理员账户：username={}, email={}，密码保持数据库现有值", adminUsername, properties.getAdmin().getEmail());
            return;
        }

        AppUser existingAdmin = userMapper.selectList(new LambdaQueryWrapper<AppUser>()
                        .eq(AppUser::getRole, "ADMIN"))
                .stream()
                .findFirst()
                .orElse(null);
        if (existingAdmin == null) {
            createUser(adminUsername, properties.getAdmin().getPassword(), properties.getAdmin().getEmail(), "ADMIN");
            log.info("已创建管理员账户：username={}, email={}", adminUsername, properties.getAdmin().getEmail());
            return;
        }

        existingAdmin.setUsernameCipher(cryptoUtil.encrypt(adminUsername));
        existingAdmin.setPasswordCipher(cryptoUtil.encrypt(properties.getAdmin().getPassword()));
        existingAdmin.setEmail(properties.getAdmin().getEmail());
        existingAdmin.setEnabled(true);
        existingAdmin.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(existingAdmin);
        log.info("已更新管理员账户：username={}, email={}", adminUsername, properties.getAdmin().getEmail());
    }

    public AppUser createUser(String username, String password, String email, String role) {
        if (findByUsername(username) != null) {
            throw new IllegalStateException("用户名已存在");
        }
        AppUser user = new AppUser();
        user.setUsernameCipher(cryptoUtil.encrypt(username));
        user.setPasswordCipher(cryptoUtil.encrypt(password));
        user.setEmail(email);
        user.setRole(role);
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    public AppUser updateUser(Long id, String username, String password, String email) {
        AppUser user = userMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        AppUser sameNameUser = findByUsername(username);
        if (sameNameUser != null && !sameNameUser.getId().equals(id)) {
            throw new IllegalStateException("用户名已存在");
        }
        user.setUsernameCipher(cryptoUtil.encrypt(username));
        if (StringUtils.hasText(password)) {
            user.setPasswordCipher(cryptoUtil.encrypt(password));
        }
        user.setEmail(email);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return user;
    }

    public AuthDtos.LoginResponse login(String username, String password) {
        AppUser user = findByUsername(username);
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (!password.equals(cryptoUtil.decrypt(user.getPasswordCipher()))) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        return new AuthDtos.LoginResponse(createToken(user.getId(), username, user.getRole()), user.getId(), username, user.getRole());
    }

    public UserPrincipal parseToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 2 || !cryptoUtil.sign(parts[0]).equals(parts[1])) {
                return null;
            }
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String[] fields = payload.split(":");
            if (fields.length < 3) {
                return null;
            }
            return new UserPrincipal(Long.parseLong(fields[0]), fields[1], fields[2]);
        } catch (Exception ex) {
            return null;
        }
    }

    public List<Map<String, Object>> listUsers() {
        return userMapper.selectList(new LambdaQueryWrapper<>()).stream()
                .map(user -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("userId", user.getId());
                    data.put("username", cryptoUtil.decrypt(user.getUsernameCipher()));
                    data.put("email", user.getEmail());
                    data.put("role", user.getRole());
                    return data;
                })
                .toList();
    }

    public AppUser getById(Long id) {
        return userMapper.selectById(id);
    }

    public String usernameOf(AppUser user) {
        return cryptoUtil.decrypt(user.getUsernameCipher());
    }

    private AppUser findByUsername(String username) {
        return userMapper.selectList(new LambdaQueryWrapper<>()).stream()
                .filter(user -> username.equals(cryptoUtil.decrypt(user.getUsernameCipher())))
                .findFirst()
                .orElse(null);
    }

    private String createToken(Long userId, String username, String role) {
        // Token keeps working until the user logs out or the token secret is changed.
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString((userId + ":" + username + ":" + role).getBytes(StandardCharsets.UTF_8));
        return payload + "." + cryptoUtil.sign(payload);
    }
}
