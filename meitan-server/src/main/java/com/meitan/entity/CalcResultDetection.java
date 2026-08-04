package com.meitan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("calc_result_detection")
public class CalcResultDetection {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String sourceDesc;
    private Integer dataPoints;
    private Double vParam;
    private Double temperature;
    private Double aParam;
    private Double critPressure;
    private Double critContent;
    private String xyArrayJson;
    private String qArrayJson;
    private String pArrayJson;
    private Integer isDanger;
    private String dangerReason;
    private String chartImage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
