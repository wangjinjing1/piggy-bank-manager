package com.piggybank.manager.controller;

import com.piggybank.manager.dto.ApiResponse;
import com.piggybank.manager.service.IpSecurityService;
import com.piggybank.manager.util.AuthContext;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/security")
public class SecurityController {
    private final IpSecurityService ipSecurityService;

    public SecurityController(IpSecurityService ipSecurityService) {
        this.ipSecurityService = ipSecurityService;
    }

    @GetMapping("/blacklist")
    public ApiResponse<?> blacklist() {
        requireAdmin();
        return ApiResponse.ok(ipSecurityService.listBlacklist());
    }

    @DeleteMapping("/blacklist/{id}")
    public ApiResponse<?> remove(@PathVariable Long id) {
        requireAdmin();
        ipSecurityService.remove(id);
        return ApiResponse.ok(true);
    }

    private void requireAdmin() {
        if (AuthContext.get() == null || !AuthContext.get().isAdmin()) {
            throw new IllegalArgumentException("需要管理员权限");
        }
    }
}
