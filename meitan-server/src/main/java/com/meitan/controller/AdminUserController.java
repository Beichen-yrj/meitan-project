package com.meitan.controller;

import com.meitan.dto.AdminUserResponse;
import com.meitan.dto.AdminUserStatistics;
import com.meitan.dto.ApiResponse;
import com.meitan.dto.BlacklistRequest;
import com.meitan.dto.PageResponse;
import com.meitan.dto.UserStatusRequest;
import com.meitan.entity.LoginLog;
import com.meitan.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ApiResponse<PageResponse<AdminUserResponse>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Boolean blacklisted,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.ok(adminUserService.listUsers(keyword, status, blacklisted, page, pageSize));
    }

    @GetMapping("/statistics")
    public ApiResponse<AdminUserStatistics> statistics() {
        return ApiResponse.ok(adminUserService.statistics());
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@RequestAttribute Long userId,
                                          @PathVariable Long id,
                                          @RequestBody UserStatusRequest request) {
        try {
            adminUserService.updateStatus(userId, id, request.getStatus());
            return ApiResponse.ok();
        } catch (RuntimeException exception) {
            return ApiResponse.error(400, exception.getMessage());
        }
    }

    @PutMapping("/{id}/blacklist")
    public ApiResponse<Void> addToBlacklist(@RequestAttribute Long userId,
                                            @PathVariable Long id,
                                            @RequestBody BlacklistRequest request) {
        try {
            adminUserService.addToBlacklist(userId, id, request.getReason());
            return ApiResponse.ok();
        } catch (RuntimeException exception) {
            return ApiResponse.error(400, exception.getMessage());
        }
    }

    @DeleteMapping("/{id}/blacklist")
    public ApiResponse<Void> removeFromBlacklist(@RequestAttribute Long userId,
                                                 @PathVariable Long id) {
        try {
            adminUserService.removeFromBlacklist(userId, id);
            return ApiResponse.ok();
        } catch (RuntimeException exception) {
            return ApiResponse.error(400, exception.getMessage());
        }
    }

    @GetMapping("/{id}/login-logs")
    public ApiResponse<PageResponse<LoginLog>> loginLogs(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        try {
            return ApiResponse.ok(adminUserService.loginLogs(id, page, pageSize));
        } catch (RuntimeException exception) {
            return ApiResponse.error(404, exception.getMessage());
        }
    }
}
