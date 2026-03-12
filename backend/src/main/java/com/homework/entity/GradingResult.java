package com.homework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("grading_results")
public class GradingResult {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long submissionId;
    private Long questionId;
    private Integer questionNo;
    private String studentAnswer;
    /** 是否正确：0-错 1-对 2-部分对 */
    private Integer isCorrect;
    private BigDecimal scoreGot;
    private BigDecimal scoreFull;
    private String feedback;
    /** 批改方式：1-自动 2-人工 */
    private Integer gradingType;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
