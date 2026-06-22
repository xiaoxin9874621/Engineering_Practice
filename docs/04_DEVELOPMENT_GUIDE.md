# 开发与二次修改说明

本文档用于说明项目结构、主要业务链路和后续可扩展点，方便交接后继续开发。

## 1. 本地运行方式

后端、数据库、Redis、OCR 服务通过 Docker Compose 启动：

```powershell
docker compose up -d --build
```

常用命令：

```powershell
docker compose ps
docker compose logs -f backend
docker compose logs -f ocr-service
docker compose down
```

Android 客户端用 Android Studio 打开 `android` 目录运行。模拟器访问后端使用：

```text
http://10.0.2.2:8080/
```

真实手机需要把 Android 端后端地址改成电脑局域网 IP。

## 2. 项目结构

```text
homework-ocr-system/
├── android/              Android Kotlin 客户端
├── backend/              Spring Boot 后端
├── ocr-service/          Flask + PaddleOCR 服务
├── test-data/            演示图片
├── docs/                 项目文档
└── docker-compose.yml
```

## 3. Android 端主要模块

| 模块 | 说明 |
| --- | --- |
| `data/api` | Retrofit 接口定义 |
| `data/model` | 请求和响应模型 |
| `data/repository` | 数据仓库，封装后端调用和演示数据 |
| `ui/student` | 学生端页面 |
| `ui/teacher` | 教师端页面 |
| `viewmodel` | 页面状态和业务调用 |

关键页面：

- 教师端作业列表：查看和创建作业。
- 教师端提交列表：查看自己班级学生上传上来的作业。
- 教师端统计分析：按班级展示平均分、及格率、提交数和批改进度。
- 学生端待完成：按 `class_members` 加载当前学生所在班级的未提交作业。
- 学生端提交记录：查看历史提交并重新提交。
- 批改详情页：展示原图、OCR 结果、每题批改结果和教师批注。

## 4. 后端主要模块

| 模块 | 说明 |
| --- | --- |
| `controller` | REST 接口 |
| `service` | 业务逻辑 |
| `mapper` | MyBatis-Plus 数据访问 |
| `entity` | 数据库实体 |
| `dto` | 请求和返回对象 |
| `config` | 安全、跨域、文件上传等配置 |

核心服务：

- `AssignmentService`：创建作业、教师作业列表、学生班级作业列表。
- `SubmissionService`：上传、重新提交、批改详情、教师提交列表、错题查询。
- `GradingService`：OCR 结果和题目对齐、自动批改。
- `AnnotationService`：教师批注新增和查询。
- `StatisticsController`：作业统计、学生趋势、教师班级统计。

## 5. 业务闭环

### 5.1 教师发布作业

1. 教师登录。
2. 创建作业，填写标题、学科、班级 ID。
3. 添加多道题目。
4. 提交到后端。
5. 后端写入 `assignments` 和 `question_items`。

### 5.2 学生提交作业

1. 学生登录。
2. 学生端调用 `GET /api/assignments/student`。
3. 后端根据 `class_members` 查询学生所在班级。
4. 后端过滤当前学生已经提交过的作业。
5. 学生选择作业，拍照或从相册选择图片。
6. 提交到 `POST /api/submissions/upload`。

待完成作业计算方式：

```text
待完成作业 = 学生所在班级已发布作业 - 当前学生已提交作业
```

### 5.3 OCR 与自动批改

1. 后端保存上传图片。
2. 调用 OCR 服务。
3. OCR 返回文本块。
4. 后端过滤姓名、班级、标题、题型提示等非答案内容。
5. 后端按题号、标准答案和关键词对齐学生答案。
6. 后端按题型和批改模式评分。
7. 写入 `submissions` 和 `grading_results`。

### 5.4 查看结果与批注

1. 学生查看提交记录和批改详情。
2. 教师查看自己班级学生提交。
3. 教师进入批改详情后可以添加批注。
4. 学生再次进入批改详情可看到教师批注。

### 5.5 错题和重新提交

- 错题接口：`GET /api/submissions/wrong`
- 重新提交：再次调用 `POST /api/submissions/upload`

重新提交会覆盖旧图片，删除旧批改结果，重新 OCR 和评分。

## 6. 已实现接口清单

| 功能 | 接口 |
| --- | --- |
| 登录 | `POST /api/auth/login` |
| 注册 | `POST /api/auth/register` |
| 教师创建作业 | `POST /api/assignments` |
| 教师作业列表 | `GET /api/assignments/teacher` |
| 学生班级待完成作业 | `GET /api/assignments/student` |
| 上传/重新提交作业 | `POST /api/submissions/upload` |
| 学生提交记录 | `GET /api/submissions/my` |
| 学生错题列表 | `GET /api/submissions/wrong` |
| 教师提交列表 | `GET /api/submissions/teacher` |
| 作业提交列表 | `GET /api/submissions/assignment/{assignmentId}` |
| 批改详情 | `GET /api/submissions/{id}/grading` |
| 查询批注 | `GET /api/annotations/submission/{submissionId}` |
| 新增批注 | `POST /api/annotations` |
| 教师班级统计 | `GET /api/statistics/teacher/classes` |

## 7. 测试图片

测试图片目录：

```text
test-data/ocr-images
```

可以拖入模拟器相册后，在学生端上传验证。

## 8. 常见二次开发任务

### 8.1 增加题型

需要修改：

- Android 创建题目页面。
- `CreateQuestionRequest`。
- 后端 `QuestionItem`。
- `GradingService.grade(...)`。
- 数据库题型说明。

### 8.2 优化 OCR 对齐

可增加题目区域框选能力：

1. 教师创建作业时为每道题配置图片区域。
2. Android 上传时传入区域信息。
3. OCR 服务按区域识别。
4. 后端按题号和区域结果对齐。

### 8.3 接入大模型评分

可在 `GradingService` 中新增语义评分模式：

```text
gradingMode = 4
```

流程：

1. OCR 获取学生答案。
2. 拼接题目、标准答案、评分标准。
3. 调用大模型返回得分和反馈。
4. 写入 `grading_results`。

### 8.4 完善班级管理

当前后端已有 `classes` 和 `class_members` 基础关系，学生端和教师端已经按该关系过滤数据。后续可继续补：

- 教师创建班级页面。
- 学生加入班级页面。
- 管理员维护班级成员页面。
