package com.meitan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("calc_result_analysis")
public class CalcResultAnalysis {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String coalType;
    private String volatile_;
    private Double vl;
    private Double pl;
    private String pArrayJson;
    private String vmArrayJson;
    private String chartStyle;
    private String statsText;
    private String chartImage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
