package com.meitan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meitan.dto.AdminUserResponse;
import com.meitan.dto.AdminUserStatistics;
import com.meitan.dto.PageResponse;
import com.meitan.entity.LoginLog;
import com.meitan.entity.User;
import com.meitan.mapper.LoginLogMapper;
import com.meitan.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserMapper userMapper;
    private final LoginLogMapper loginLogMapper;

    public PageResponse<AdminUserResponse> listUsers(String keyword,
                                                     Integer status,
                                                     Boolean blacklisted,
                                                     long page,
                                                     long pageSize) {
        long safePage = Math.max(1, page);
        long safeSize = Math.min(100, Math.max(1, pageSize));
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            query.and(wrapper -> wrapper
                    .like(User::getUsername, value)
                    .or().like(User::getRealName, value)
                    .or().like(User::getEmail, value)
                    .or().like(User::getPhone, value));
        }
        if (status != null) {
            query.eq(User::getStatus, status);
        }
        if (blacklisted != null) {
            query.eq(User::getBlacklisted, blacklisted ? 1 : 0);
        }
        query.orderByDesc(User::getLastLoginTime).orderByDesc(User::getCreateTime);

        Page<User> result = userMapper.selectPage(new Page<>(safePage, safeSize), query);
        List<AdminUserResponse> records = result.getRecords().stream()
                .map(this::toResponse)
                .toList();
        return PageResponse.of(result, records);
    }

    public AdminUserStatistics statistics() {
        long total = userMapper.selectCount(null);
        long enabled = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1));
        long disabled = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 0));
        long blacklisted = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getBlacklisted, 1));
        return AdminUserStatistics.builder()
                .totalUsers(total)
                .enabledUsers(enabled)
                .disabledUsers(disabled)
                .blacklistedUsers(blacklisted)
                .todayLoginCount(loginLogMapper.countTodaySuccess())
                .todayFailedCount(loginLogMapper.countTodayFailure())
                .build();
    }

    public void updateStatus(Long operatorId, Long targetId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new RuntimeException("账号状态参数不正确");
        }
        User target = getManageableUser(operatorId, targetId);
        target.setStatus(status);
        userMapper.updateById(target);
    }

    public void addToBlacklist(Long operatorId, Long targetId, String reason) {
        User target = getManageableUser(operatorId, targetId);
        target.setBlacklisted(1);
        target.setBlacklistReason(StringUtils.hasText(reason) ? truncate(reason.trim(), 500) : "管理员加入黑名单");
        userMapper.updateById(target);
    }

    public void removeFromBlacklist(Long operatorId, Long targetId) {
        User target = getManageableUser(operatorId, targetId);
        target.setBlacklisted(0);
        target.setBlacklistReason("");
        userMapper.updateById(target);
    }

    public PageResponse<LoginLog> loginLogs(Long userId, long page, long pageSize) {
        if (userMapper.selectById(userId) == null) {
            throw new RuntimeException("用户不存在");
        }
        Page<LoginLog> result = loginLogMapper.selectPage(
                new Page<>(Math.max(1, page), Math.min(100, Math.max(1, pageSize))),
                new LambdaQueryWrapper<LoginLog>()
                        .eq(LoginLog::getUserId, userId)
                        .orderByDesc(LoginLog::getLoginTime)
        );
        return PageResponse.of(result, result.getRecords());
    }

    private User getManageableUser(Long operatorId, Long targetId) {
        User target = userMapper.selectById(targetId);
        if (target == null) {
            throw new RuntimeException("用户不存在");
        }
        if (operatorId.equals(targetId)) {
            throw new RuntimeException("不能对当前登录的管理员账号执行此操作");
        }
        if ("ADMIN".equalsIgnoreCase(target.getRole())) {
            throw new RuntimeException("不能修改其他管理员账号");
        }
        return target;
    }

    private AdminUserResponse toResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .blacklisted(user.getBlacklisted())
                .blacklistReason(user.getBlacklistReason())
                .lastLoginTime(user.getLastLoginTime())
                .lastLoginIp(user.getLastLoginIp())
                .loginCount(user.getLoginCount())
                .createTime(user.getCreateTime())
                .updateTime(user.getUpdateTime())
                .build();
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
