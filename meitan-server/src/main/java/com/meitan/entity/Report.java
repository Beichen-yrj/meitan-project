package com.meitan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("report")
public class Report {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String taskIds;
    private String summaryJson;
    private String filePath;
    private String fileFormat;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
