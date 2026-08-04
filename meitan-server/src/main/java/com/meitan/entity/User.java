package com.meitan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;
    private String realName;
    private String email;
    private String phone;

    /** 角色：ADMIN / USER */
    private String role;

    /** 状态：1-启用 0-禁用 */
    private Integer status;

    /** 是否在黑名单：1-是 0-否 */
    @TableField("is_blacklisted")
    private Integer blacklisted;

    private String blacklistReason;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private Integer loginCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
