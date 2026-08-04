package com.meitan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("task_calculation")
public class TaskCalculation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 模块：ANALYSIS / STATISTICS / DETECTION */
    private String moduleType;

    private Long fileId;
    private String paramsJson;

    /** 状态：PENDING / RUNNING / SUCCESS / FAILED */
    private String status;

    private String errorMsg;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
