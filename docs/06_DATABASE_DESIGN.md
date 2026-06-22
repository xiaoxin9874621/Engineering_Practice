# 数据库设计说明

数据库名称：

```text
homework_ocr
```

初始化脚本：

```text
backend/src/main/resources/db/init.sql
```

## 1. 表结构总览

| 表名 | 说明 |
| --- | --- |
| `users` | 用户表 |
| `classes` | 班级表 |
| `class_members` | 班级成员表 |
| `assignments` | 作业表 |
| `question_items` | 作业题目表 |
| `submissions` | 学生提交表 |
| `ocr_results` | OCR 识别结果表，预留 |
| `grading_results` | 批改结果表 |
| `annotations` | 教师批注表，已接入批改详情 |
| `statistics` | 统计表，预留；当前统计由接口实时聚合 |

## 2. 核心表说明

### 2.1 users

保存系统账号。

| 字段 | 说明 |
| --- | --- |
| `id` | 用户 ID |
| `username` | 登录用户名 |
| `password` | BCrypt 加密密码 |
| `real_name` | 真实姓名 |
| `role` | 角色：1 学生，2 教师，3 管理员 |
| `status` | 状态：0 禁用，1 正常 |

### 2.2 classes

保存班级信息。

| 字段 | 说明 |
| --- | --- |
| `id` | 班级 ID |
| `class_name` | 班级名称 |
| `teacher_id` | 负责教师 ID |

教师端作业、提交记录和统计分析都会按负责教师过滤。

### 2.3 class_members

保存学生和班级的关系。

| 字段 | 说明 |
| --- | --- |
| `class_id` | 班级 ID |
| `student_id` | 学生 ID |

学生端待完成作业使用该表查找当前学生所在班级。

### 2.4 assignments

保存作业主信息。

| 字段 | 说明 |
| --- | --- |
| `id` | 作业 ID |
| `title` | 作业标题 |
| `description` | 作业说明 |
| `class_id` | 所属班级 |
| `teacher_id` | 创建教师 |
| `subject` | 学科 |
| `deadline` | 截止时间 |
| `total_score` | 总分 |
| `status` | 0 草稿，1 发布，2 结束 |

### 2.5 question_items

保存作业下的题目。

| 字段 | 说明 |
| --- | --- |
| `id` | 题目 ID |
| `assignment_id` | 所属作业 |
| `question_no` | 题号 |
| `question_type` | 1 选择题，2 填空题，3 简答题 |
| `question_text` | 题目内容 |
| `answer_key` | 标准答案 |
| `score` | 分值 |
| `grading_mode` | 1 精确匹配，2 关键词匹配，3 人工审核 |
| `keywords` | 关键词 JSON 字符串 |

### 2.6 submissions

保存学生提交记录。

| 字段 | 说明 |
| --- | --- |
| `id` | 提交 ID |
| `assignment_id` | 作业 ID |
| `student_id` | 学生 ID |
| `image_url` | 图片访问路径 |
| `image_path` | 图片本地存储路径 |
| `status` | 0 待识别，1 识别中，2 识别完成，3 批改中，4 批改完成，5 失败 |
| `total_score` | 最终得分 |
| `submit_time` | 提交时间 |
| `graded_time` | 批改完成时间 |

同一学生对同一作业重新提交时，会覆盖旧图片并清空旧批改结果，避免待完成数量和提交记录重复。

### 2.7 grading_results

保存每一道题的批改结果。

| 字段 | 说明 |
| --- | --- |
| `id` | 批改结果 ID |
| `submission_id` | 提交 ID |
| `question_id` | 题目 ID |
| `question_no` | 题号 |
| `student_answer` | OCR 识别出的学生答案 |
| `is_correct` | 0 错误，1 正确，2 部分正确 |
| `score_got` | 得分 |
| `score_full` | 满分 |
| `feedback` | 批改反馈 |
| `grading_type` | 1 自动批改，2 人工批改 |

### 2.8 annotations

保存教师对某次提交的批注。

| 字段 | 说明 |
| --- | --- |
| `id` | 批注 ID |
| `submission_id` | 提交 ID |
| `teacher_id` | 批注教师 ID |
| `content` | 批注内容 |
| `created_at` | 创建时间 |

教师端可新增批注，教师端和学生端均可在批改详情查看。

## 3. 主要关系

```text
users(teacher) 1 -> N classes
classes 1 -> N class_members
users(student) 1 -> N class_members
classes 1 -> N assignments
users(teacher) 1 -> N assignments
assignments 1 -> N question_items
users(student) 1 -> N submissions
assignments 1 -> N submissions
submissions 1 -> N grading_results
submissions 1 -> N annotations
question_items 1 -> N grading_results
```

## 4. 演示数据

初始化账号：

| 用户名 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `Demo@123` | 管理员 |
| `teacher` | `Demo@123` | 教师 |
| `student` | `Demo@123` | 学生 |

初始化班级：

```text
class_id = 1
```

演示学生通过 `class_members` 加入该班级，学生端会按真实班级关系加载作业。

## 5. 数据重置

如果使用 Docker：

```powershell
docker compose down -v
docker compose up -d --build
```

这会删除已有数据并重新执行初始化 SQL。
