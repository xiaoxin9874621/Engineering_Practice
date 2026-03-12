package com.homework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("submissions")
public class Submission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long assignmentId;
    private Long studentId;
    private String imageUrl;
    private String imagePath;
    /** 状态：0-待识别 1-识别中 2-识别完成 3-批改中 4-批改完成 5-失败 */
    private Integer status;
    private BigDecimal totalScore;
    private LocalDateTime submitTime;
    private LocalDateTime gradedTime;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
