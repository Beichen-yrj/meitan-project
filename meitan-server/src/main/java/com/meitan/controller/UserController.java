package com.meitan.controller;

import com.meitan.dto.ApiResponse;
import com.meitan.dto.ChangePasswordRequest;
import com.meitan.dto.LoginResponse;
import com.meitan.entity.User;
import com.meitan.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    @GetMapping("/profile")
    public ApiResponse<LoginResponse> profile(@RequestAttribute Long userId) {
        User user = authService.getUserById(userId);
        LoginResponse resp = LoginResponse.builder()
                .token(null)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole())
                .build();
        return ApiResponse.ok(resp);
    }

    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(@RequestAttribute Long userId, @RequestBody User param) {
        User user = authService.getUserById(userId);
        if (user == null) {
            return ApiResponse.error(404, "用户不存在");
        }
        user.setRealName(param.getRealName());
        user.setEmail(param.getEmail());
        user.setPhone(param.getPhone());
        authService.updateUser(user);
        return ApiResponse.ok();
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@RequestAttribute Long userId,
                                            @RequestBody ChangePasswordRequest request) {
        try {
            authService.changePassword(userId, request);
            return ApiResponse.ok();
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }
}
