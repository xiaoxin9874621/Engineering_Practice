package com.homework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("assignments")
public class Assignment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private Long classId;
    private Long teacherId;
    private String subject;
    private LocalDateTime deadline;
    private BigDecimal totalScore;
    /** 状态：0-草稿 1-发布 2-结束 */
    private Integer status;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
