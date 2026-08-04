package com.meitan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_login_log")
public class LoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private LocalDateTime loginTime;
    private String loginIp;
    private String userAgent;

    /** 登录结果：1-成功 0-失败 */
    private Integer success;
    private String failureReason;
}
