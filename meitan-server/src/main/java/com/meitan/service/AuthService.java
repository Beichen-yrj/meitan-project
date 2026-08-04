package com.meitan.service;

import com.meitan.dto.ChangePasswordRequest;
import com.meitan.dto.LoginRequest;
import com.meitan.dto.LoginResponse;
import com.meitan.dto.RegisterRequest;
import com.meitan.entity.LoginLog;
import com.meitan.entity.User;
import com.meitan.mapper.LoginLogMapper;
import com.meitan.mapper.UserMapper;
import com.meitan.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final LoginLogMapper loginLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public LoginResponse login(LoginRequest request, String loginIp, String userAgent) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        LocalDateTime loginTime = LocalDateTime.now();
        User user = userMapper.findByUsername(username);
        if (user == null) {
            recordLogin(null, username, loginTime, loginIp, userAgent, false, "用户名不存在");
            throw new RuntimeException("用户名或密码错误");
        }
        if (Integer.valueOf(1).equals(user.getBlacklisted())) {
            recordLogin(user.getId(), username, loginTime, loginIp, userAgent, false, "账号已加入黑名单");
            throw new RuntimeException("账号已加入黑名单，请联系管理员");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            recordLogin(user.getId(), username, loginTime, loginIp, userAgent, false, "账号已禁用");
            throw new RuntimeException("账号已禁用，请联系管理员");
        }

        String dbPassword = user.getPassword();
        boolean matched;

        if (dbPassword.startsWith("$2")) {
            matched = passwordEncoder.matches(request.getPassword(), dbPassword);
        } else {
            // 兼容旧的明文密码，首次登录成功后自动升级为 BCrypt。
            matched = request.getPassword().equals(dbPassword);
            if (matched) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
                userMapper.updateById(user);
            }
        }

        if (!matched) {
            recordLogin(user.getId(), username, loginTime, loginIp, userAgent, false, "密码错误");
            throw new RuntimeException("密码错误");
        }

        userMapper.recordSuccessfulLogin(user.getId(), loginTime, truncate(loginIp, 64));
        recordLogin(user.getId(), username, loginTime, loginIp, userAgent, true, null);
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole())
                .build();
    }

    public void register(RegisterRequest request) {
        User existUser = userMapper.findByUsername(request.getUsername());
        if (existUser != null) {
            throw new RuntimeException("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setRole("USER");
        user.setStatus(1);
        user.setBlacklisted(0);
        user.setLoginCount(0);
        userMapper.insert(user);
    }

    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }

    public void updateUser(User user) {
        userMapper.updateById(user);
    }

    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        String dbPassword = user.getPassword();
        boolean matched;
        if (dbPassword.startsWith("$2")) {
            matched = passwordEncoder.matches(request.getOldPassword(), dbPassword);
        } else {
            matched = request.getOldPassword().equals(dbPassword);
        }
        if (!matched) {
            throw new RuntimeException("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
    }

    private void recordLogin(Long userId,
                             String username,
                             LocalDateTime loginTime,
                             String loginIp,
                             String userAgent,
                             boolean success,
                             String failureReason) {
        LoginLog log = new LoginLog();
        log.setUserId(userId);
        log.setUsername(truncate(username.isBlank() ? "(空用户名)" : username, 50));
        log.setLoginTime(loginTime);
        log.setLoginIp(truncate(loginIp, 64));
        log.setUserAgent(truncate(userAgent, 500));
        log.setSuccess(success ? 1 : 0);
        log.setFailureReason(truncate(failureReason, 255));
        loginLogMapper.insert(log);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
