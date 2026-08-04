package com.meitan.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminUserResponse {
    private Long id;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private String role;
    private Integer status;
    private Integer blacklisted;
    private String blacklistReason;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private Integer loginCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
