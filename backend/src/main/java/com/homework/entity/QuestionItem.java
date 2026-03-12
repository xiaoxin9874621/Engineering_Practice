package com.homework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("question_items")
public class QuestionItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long assignmentId;
    private Integer questionNo;
    /** 题型：1-选择 2-填空 3-简答 */
    private Integer questionType;
    private String questionText;
    private String answerKey;
    private BigDecimal score;
    /** 批改模式：1-精确匹配 2-关键词匹配 3-人工审核 */
    private Integer gradingMode;
    private String keywords;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
