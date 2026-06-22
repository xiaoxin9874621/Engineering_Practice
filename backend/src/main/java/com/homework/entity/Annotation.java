package com.homework.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("annotations")
public class Annotation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long submissionId;
    private Long teacherId;
    private String content;
    private Integer positionX;
    private Integer positionY;
    private String color;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
