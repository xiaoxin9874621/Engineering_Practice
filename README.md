# 手写作业OCR识别与基础批改系统

基于 Spring Boot 3.0 + PaddleOCR + Android (Kotlin) 的完整手写作业识别与批改解决方案。

## 项目结构

```
homework-ocr-system/
├── backend/          # Spring Boot 3.0 后端服务
├── ocr-service/      # PaddleOCR Flask 微服务（Python）
├── android/          # Android 客户端（Kotlin + MVVM）
├── docker-compose.yml
└── README.md
```

---

## 快速启动

### 1. 启动基础服务（MySQL + Redis + OCR）

```bash
docker-compose up -d mysql redis ocr-service
```

等待 MySQL 初始化完成（约30秒）再启动后端。

### 2. 启动 Spring Boot 后端

**前提条件**：JDK 17、Maven 3.6+

```bash
cd backend
mvn spring-boot:run
```

后端启动后访问：
- Swagger UI: http://localhost:8080/doc.html
- API 文档：http://localhost:8080/v3/api-docs

### 3. 单独启动 OCR 服务（不用Docker）

**前提条件**：Python 3.8+

```bash
cd ocr-service
pip install -r requirements.txt
python app.py
```

健康检查：http://localhost:5000/api/health

### 4. 运行 Android 客户端

1. 用 Android Studio 打开 `android/` 目录
2. 连接 Android 设备（或启动模拟器）
3. 修改 `app/build.gradle.kts` 中的 `BASE_URL`：
   - 模拟器访问本机：`http://10.0.2.2:8080/`
   - 真机访问：改为后端服务器 IP
4. 点击 Run

---

## 技术栈

| 层次 | 技术 |
|------|------|
| Android 客户端 | Kotlin / MVVM / LiveData / Hilt / Retrofit2 / Coroutine / Room |
| 后端 | Spring Boot 3.0 / Spring Security (JWT) / MyBatis-Plus / Swagger 3.0 / Quartz |
| OCR 服务 | PaddleOCR 2.6 / Flask / OpenCV |
| 数据库 | MySQL 8.0 / Redis 7.0 |

---

## 主要功能模块

### 认证模块
- POST `/api/auth/register` — 注册（选择教师/学生角色）
- POST `/api/auth/login` — 登录（返回 JWT Token）

### 作业管理
- POST `/api/assignments` — 教师创建作业（含题目和标准答案）
- GET `/api/assignments/teacher` — 教师查看自己的作业
- GET `/api/assignments/class/{classId}` — 学生查看班级作业

### 提交与批改
- POST `/api/submissions/upload` — 学生上传作业图片（自动触发OCR+批改）
- GET `/api/submissions/{id}/grading` — 查看批改详情

### 统计分析
- GET `/api/statistics/assignment/{id}` — 作业统计（平均分/及格率等）
- GET `/api/statistics/student/{id}/trend` — 学生历次成绩趋势

---

## 批改引擎说明

| 题型 | 批改模式 | 说明 |
|------|---------|------|
| 选择题 | 精确匹配（模式1） | 忽略大小写和空格 |
| 填空题 | 精确匹配（模式1） | 规范化后比较 |
| 简答题 | 关键词匹配（模式2） | 按关键词出现比例给分 |
| 其他 | 人工审核（模式3） | 不自动打分 |

---

## 数据库配置

数据库名：`homework_ocr`
初始化 SQL：`backend/src/main/resources/db/init.sql`

修改链接信息：`backend/src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/homework_ocr?...
    username: root
    password: 123456
```

---

## 常见问题

**Q：OCR识别准确率如何提升？**
A：PaddleOCR 首次运行会自动下载模型（约500MB）。可在 `ocr_engine.py` 中开启 GPU 模式（`use_gpu=True`）提升速度和准确率。

**Q：Android 连接不到后端？**
A：确保手机/模拟器和后端在同一网络，修改 `BASE_URL` 为正确 IP。模拟器使用 `10.0.2.2` 代替 `localhost`。

**Q：图片上传大小限制？**
A：默认 10MB，可在 `application.yml` 和 Android 的 `OcrService` 中调整。
