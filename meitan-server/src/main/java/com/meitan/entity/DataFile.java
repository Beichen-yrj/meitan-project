package com.meitan.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("data_file")
public class DataFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 模块：ANALYSIS / STATISTICS / DETECTION */
    private String moduleType;

    private String fileName;

    /** 服务端物理路径不向前端暴露。 */
    @JsonIgnore
    private String filePath;

    private Long fileSize;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime uploadTime;
}
