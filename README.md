# 手写作业 OCR 识别与自动批改系统

这是一个可演示、可交接的作业 OCR 批改闭环项目，包含 Android 客户端、Spring Boot 后端、PaddleOCR 微服务、MySQL 和 Redis。

## 快速启动

在项目根目录运行：

```powershell
docker compose up -d --build
```

启动后访问：

```text
后端接口文档：http://localhost:8080/doc.html
OCR 健康检查：http://localhost:5000/api/health
Android 模拟器访问后端：http://10.0.2.2:8080/
```

Android Studio 中打开 `android` 目录，选择 Pixel 6 / API 34 模拟器，点击绿色运行按钮安装 App。

## 演示账号

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| 管理员 | `admin` | `Demo@123` |
| 教师 | `teacher` | `Demo@123` |
| 学生 | `student` | `Demo@123` |

## 核心功能

- 教师创建多题作业，支持选择题、填空题、简答题。
- 教师负责自己班级的作业和学生提交。
- 教师端可查看作业列表、提交列表、学生批改详情。
- 教师端可在批改详情中添加批注，学生端可查看批注。
- 教师端统计分析按班级展示平均分、及格率、提交数和批改进度。
- 学生按班级成员关系查看待完成作业，不再依赖固定班级 ID。
- 学生可拍照或从相册选择手写作业图片，并在提交前预览。
- 后端保存原图，调用 OCR 服务识别手写内容。
- 后端按题号、标准答案和关键词对齐 OCR 结果，减少答案错位。
- 自动批改并保存每题得分、对错、反馈和总分。
- 学生提交后，待完成数量会减少。
- 学生可查看提交记录、批改详情、历史成绩、错题列表。
- 学生可对同一作业重新提交，后端会覆盖旧提交并重新 OCR 和批改。

## 文档入口

- [项目总览](docs/01_PROJECT_OVERVIEW.md)
- [部署与运行说明](docs/02_DEPLOYMENT_GUIDE.md)
- [接口说明](docs/03_API_REFERENCE.md)
- [开发与二次修改说明](docs/04_DEVELOPMENT_GUIDE.md)
- [演示流程脚本](docs/05_DEMO_SCRIPT.md)
- [数据库设计说明](docs/06_DATABASE_DESIGN.md)
- [用户操作手册](docs/07_USER_MANUAL.md)

## 项目结构

```text
homework-ocr-system/
├── android/              Android 客户端
├── backend/              Spring Boot 后端
├── ocr-service/          Flask + PaddleOCR 微服务
├── test-data/            演示测试图片
├── docs/                 项目文档
└── docker-compose.yml    Docker 编排文件
```
