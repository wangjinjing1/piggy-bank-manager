package com.piggybank.manager.controller;

import com.piggybank.manager.dto.ApiResponse;
import com.piggybank.manager.dto.AuthDtos;
import com.piggybank.manager.service.UserService;
import com.piggybank.manager.util.AuthContext;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<?> list() {
        requireAdmin();
        return ApiResponse.ok(userService.listUsers());
    }

    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody AuthDtos.CreateUserRequest request) {
        requireAdmin();
        var user = userService.createUser(request.getUsername(), request.getPassword(), request.getEmail(), "USER");
        return ApiResponse.ok(userMap(user.getId(), request.getUsername(), user.getRole()));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable Long id, @Valid @RequestBody AuthDtos.UpdateUserRequest request) {
        requireAdmin();
        var user = userService.updateUser(id, request.getUsername(), request.getPassword(), request.getEmail());
        return ApiResponse.ok(userMap(user.getId(), request.getUsername(), user.getRole()));
    }

    private Map<String, Object> userMap(Long userId, String username, String role) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("username", username);
        data.put("role", role);
        return data;
    }

    private void requireAdmin() {
        if (AuthContext.get() == null || !AuthContext.get().isAdmin()) {
            throw new IllegalArgumentException("需要管理员权限");
        }
    }
}
