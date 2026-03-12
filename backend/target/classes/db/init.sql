-- 手写作业OCR识别与批改系统 数据库初始化脚本
-- 数据库：homework_ocr

CREATE DATABASE IF NOT EXISTS homework_ocr DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE homework_ocr;

-- 用户表
CREATE TABLE IF NOT EXISTS `users` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`     VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    `password`     VARCHAR(255) NOT NULL COMMENT '密码（加密）',
    `real_name`    VARCHAR(50)  NOT NULL COMMENT '真实姓名',
    `email`        VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `phone`        VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `role`         TINYINT      NOT NULL DEFAULT 1 COMMENT '角色：1-学生 2-教师 3-管理员',
    `avatar`       VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-删除',
    `created_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 班级表
CREATE TABLE IF NOT EXISTS `classes` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '班级ID',
    `class_name`   VARCHAR(100) NOT NULL COMMENT '班级名称',
    `class_code`   VARCHAR(20)  NOT NULL UNIQUE COMMENT '班级邀请码',
    `teacher_id`   BIGINT       NOT NULL COMMENT '任课教师ID',
    `description`  VARCHAR(500) DEFAULT NULL COMMENT '描述',
    `deleted`      TINYINT      NOT NULL DEFAULT 0,
    `created_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_teacher_id` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级表';

-- 班级成员关系表
CREATE TABLE IF NOT EXISTS `class_members` (
    `id`           BIGINT   NOT NULL AUTO_INCREMENT,
    `class_id`     BIGINT   NOT NULL COMMENT '班级ID',
    `student_id`   BIGINT   NOT NULL COMMENT '学生ID',
    `join_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_class_student` (`class_id`, `student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级成员关系表';

-- 作业表
CREATE TABLE IF NOT EXISTS `assignments` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '作业ID',
    `title`         VARCHAR(200) NOT NULL COMMENT '作业标题',
    `description`   TEXT         DEFAULT NULL COMMENT '作业说明',
    `class_id`      BIGINT       NOT NULL COMMENT '班级ID',
    `teacher_id`    BIGINT       NOT NULL COMMENT '教师ID',
    `subject`       VARCHAR(50)  DEFAULT NULL COMMENT '学科',
    `deadline`      DATETIME     DEFAULT NULL COMMENT '截止时间',
    `total_score`   DECIMAL(6,2) NOT NULL DEFAULT 100 COMMENT '总分',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-草稿 1-发布 2-结束',
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    `created_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_class_id` (`class_id`),
    KEY `idx_teacher_id` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业表';

-- 题目条目表（作业包含的题目及标准答案）
CREATE TABLE IF NOT EXISTS `question_items` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '题目ID',
    `assignment_id`   BIGINT       NOT NULL COMMENT '作业ID',
    `question_no`     INT          NOT NULL COMMENT '题号',
    `question_type`   TINYINT      NOT NULL COMMENT '题型：1-选择 2-填空 3-简答',
    `question_text`   TEXT         DEFAULT NULL COMMENT '题目内容',
    `answer_key`      TEXT         NOT NULL COMMENT '标准答案',
    `score`           DECIMAL(6,2) NOT NULL COMMENT '分值',
    `grading_mode`    TINYINT      NOT NULL DEFAULT 1 COMMENT '批改模式：1-精确匹配 2-关键词匹配 3-人工审核',
    `keywords`        TEXT         DEFAULT NULL COMMENT '关键词（JSON数组，用于简答题）',
    `created_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_assignment_id` (`assignment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目条目表';

-- 提交记录表
CREATE TABLE IF NOT EXISTS `submissions` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '提交ID',
    `assignment_id`   BIGINT       NOT NULL COMMENT '作业ID',
    `student_id`      BIGINT       NOT NULL COMMENT '学生ID',
    `image_url`       VARCHAR(500) NOT NULL COMMENT '作业图片URL',
    `image_path`      VARCHAR(500) NOT NULL COMMENT '作业图片本地路径',
    `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0-待识别 1-识别中 2-识别完成 3-批改中 4-批改完成 5-失败',
    `total_score`     DECIMAL(6,2) DEFAULT NULL COMMENT '最终得分',
    `submit_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    `graded_time`     DATETIME     DEFAULT NULL COMMENT '批改完成时间',
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    `created_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_assignment_student` (`assignment_id`, `student_id`),
    KEY `idx_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提交记录表';

-- OCR识别结果表
CREATE TABLE IF NOT EXISTS `ocr_results` (
    `id`              BIGINT NOT NULL AUTO_INCREMENT COMMENT 'OCR结果ID',
    `submission_id`   BIGINT NOT NULL COMMENT '提交ID',
    `question_no`     INT    NOT NULL COMMENT '题号',
    `raw_text`        TEXT   NOT NULL COMMENT 'OCR原始识别文本',
    `confidence`      FLOAT  DEFAULT NULL COMMENT '置信度',
    `bounding_box`    VARCHAR(200) DEFAULT NULL COMMENT '文字区域坐标（JSON）',
    `process_time_ms` INT    DEFAULT NULL COMMENT '识别耗时（毫秒）',
    `created_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_submission_id` (`submission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OCR识别结果表';

-- 批改结果详情表
CREATE TABLE IF NOT EXISTS `grading_results` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '批改结果ID',
    `submission_id`   BIGINT       NOT NULL COMMENT '提交ID',
    `question_id`     BIGINT       NOT NULL COMMENT '题目ID',
    `question_no`     INT          NOT NULL COMMENT '题号',
    `student_answer`  TEXT         DEFAULT NULL COMMENT '学生答案（OCR识别结果）',
    `is_correct`      TINYINT      DEFAULT NULL COMMENT '是否正确：0-错 1-对 2-部分对',
    `score_got`       DECIMAL(6,2) DEFAULT NULL COMMENT '得分',
    `score_full`      DECIMAL(6,2) NOT NULL COMMENT '满分',
    `feedback`        TEXT         DEFAULT NULL COMMENT '批改反馈',
    `grading_type`    TINYINT      NOT NULL DEFAULT 1 COMMENT '批改方式：1-自动 2-人工',
    `created_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_submission_id` (`submission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批改结果详情表';

-- 批注表（教师手动批注）
CREATE TABLE IF NOT EXISTS `annotations` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '批注ID',
    `submission_id`  BIGINT       NOT NULL COMMENT '提交ID',
    `teacher_id`     BIGINT       NOT NULL COMMENT '教师ID',
    `content`        TEXT         NOT NULL COMMENT '批注内容',
    `position_x`     INT          DEFAULT NULL COMMENT '批注X坐标',
    `position_y`     INT          DEFAULT NULL COMMENT '批注Y坐标',
    `color`          VARCHAR(10)  DEFAULT '#FF0000' COMMENT '批注颜色',
    `created_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_submission_id` (`submission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批注表';

-- 统计汇总表（Quartz定时任务更新）
CREATE TABLE IF NOT EXISTS `statistics_summary` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT,
    `assignment_id`    BIGINT       NOT NULL COMMENT '作业ID',
    `class_id`         BIGINT       NOT NULL COMMENT '班级ID',
    `total_students`   INT          NOT NULL DEFAULT 0 COMMENT '总学生数',
    `submitted_count`  INT          NOT NULL DEFAULT 0 COMMENT '已提交数',
    `graded_count`     INT          NOT NULL DEFAULT 0 COMMENT '已批改数',
    `avg_score`        DECIMAL(6,2) DEFAULT NULL COMMENT '平均分',
    `max_score`        DECIMAL(6,2) DEFAULT NULL COMMENT '最高分',
    `min_score`        DECIMAL(6,2) DEFAULT NULL COMMENT '最低分',
    `pass_rate`        DECIMAL(5,2) DEFAULT NULL COMMENT '及格率',
    `updated_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_assignment_class` (`assignment_id`, `class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统计汇总表';

-- 初始化管理员账号（密码：Admin@123 的BCrypt加密值）
INSERT INTO `users` (`username`, `password`, `real_name`, `role`, `status`)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', 3, 1)
ON DUPLICATE KEY UPDATE `username` = `username`;
