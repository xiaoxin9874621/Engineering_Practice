# 部署与运行说明

## 环境要求

推荐环境：

| 软件 | 推荐版本 |
| --- | --- |
| Windows | Windows 10 / Windows 11 |
| Docker Desktop | 最新稳定版 |
| Android Studio | 最新稳定版 |
| JDK | 17 |
| Android SDK | API 34 |

如果不使用 Docker 单独运行后端，还需要 Maven、MySQL 8、Redis 7、Python 3.10。

## Docker 一键启动

在项目根目录执行：

```powershell
cd "D:\Life and Games\OCR\homework-ocr-system"
docker compose up -d --build
```

检查服务：

```powershell
docker compose ps
```

正常应看到：

```text
homework_mysql
homework_redis
homework_ocr
homework_backend
```

## 服务地址

| 服务 | 地址 |
| --- | --- |
| 后端接口文档 | `http://localhost:8080/doc.html` |
| 后端 OpenAPI | `http://localhost:8080/v3/api-docs` |
| OCR 健康检查 | `http://localhost:5000/api/health` |
| MySQL | `localhost:3307` |
| Redis | `localhost:6379` |

## 初始账号

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| 管理员 | `admin` | `Demo@123` |
| 教师 | `teacher` | `Demo@123` |
| 学生 | `student` | `Demo@123` |

## Android 模拟器运行

当前 Android Debug 包默认后端地址：

```text
http://10.0.2.2:8080/
```

运行步骤：

1. Android Studio 打开 `android/` 目录。
2. 创建或启动 Pixel 6 / API 34 模拟器。
3. 点击绿色运行按钮安装 App。
4. 使用演示账号登录。

## 真实手机运行

真实手机不能访问 `10.0.2.2`，需要改为电脑局域网 IP。

步骤：

1. 手机和电脑连接同一个 Wi-Fi。
2. 查看电脑局域网 IP，例如 `192.168.1.23`。
3. 修改 `android/app/build.gradle.kts`：

```kotlin
buildConfigField("String", "BASE_URL", "\"http://192.168.1.23:8080/\"")
```

4. 重新运行或打包 APK。
5. Windows 防火墙允许 8080 端口访问。

## 打包 APK

在 Android Studio 中：

```text
Build -> Build Bundle(s) / APK(s) -> Build APK(s)
```

生成路径：

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

## 常用 Docker 命令

查看日志：

```powershell
docker compose logs -f backend
docker compose logs -f ocr-service
```

停止：

```powershell
docker compose down
```

清空数据库并重启：

```powershell
docker compose down -v
docker compose up -d --build
```

注意：`down -v` 会删除数据库数据，包括作业、提交和成绩。

## 常见问题

### PowerShell 路径有空格无法进入

路径有空格时必须加引号：

```powershell
cd "D:\Life and Games\OCR\homework-ocr-system"
```

### Docker Desktop 中只看到 OCR

说明只启动了单个服务。完整启动：

```powershell
docker compose up -d --build
```

### App 登录失败

先确认后端是否能打开：

```text
http://localhost:8080/doc.html
```

模拟器中访问后端使用：

```text
http://10.0.2.2:8080/
```

### OCR 第一次很慢

第一次启动 OCR 会加载模型，等待 1 到 3 分钟后再测试。
