# 接口说明

后端基础地址：

```text
http://localhost:8080/
```

Android 模拟器访问地址：

```text
http://10.0.2.2:8080/
```

登录后的接口需要请求头：

```text
Authorization: Bearer <accessToken>
```

统一返回格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

## 1. 认证接口

### 登录

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "username": "student",
  "password": "Demo@123"
}
```

| role | 说明 |
| --- | --- |
| 1 | 学生 |
| 2 | 教师 |
| 3 | 管理员 |

### 注册

```http
POST /api/auth/register
Content-Type: application/json
```

```json
{
  "username": "new_student",
  "password": "123456",
  "realName": "新学生",
  "email": "student@example.com",
  "phone": "13800000000",
  "role": 1
}
```

## 2. 作业接口

### 教师创建作业

```http
POST /api/assignments
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "title": "数学周测：一次函数",
  "description": "Android 客户端创建",
  "classId": 1,
  "subject": "数学",
  "questions": [
    {
      "questionNo": 1,
      "questionType": 1,
      "questionText": "选择正确答案",
      "answerKey": "A",
      "score": 10,
      "gradingMode": 1,
      "keywords": null
    },
    {
      "questionNo": 2,
      "questionType": 2,
      "questionText": "填写关键概念",
      "answerKey": "光合作用",
      "score": 20,
      "gradingMode": 1,
      "keywords": null
    },
    {
      "questionNo": 3,
      "questionType": 3,
      "questionText": "简述一次函数图像特点",
      "answerKey": "两点,直线,函数",
      "score": 70,
      "gradingMode": 2,
      "keywords": "[\"两点\",\"直线\",\"函数\"]"
    }
  ]
}
```

| 字段 | 说明 |
| --- | --- |
| `questionType` | 1 选择题，2 填空题，3 简答题 |
| `gradingMode` | 1 精确匹配，2 关键词匹配，3 人工审核 |
| `keywords` | 简答题关键词 JSON 字符串 |

### 教师查看自己的作业

```http
GET /api/assignments/teacher?page=1&size=10
Authorization: Bearer <token>
```

### 学生查看自己班级的待完成作业

```http
GET /api/assignments/student?page=1&size=10
Authorization: Bearer <token>
```

该接口会根据 `class_members` 表查询当前学生所在班级，并过滤已经提交过的作业。

### 兼容接口：按班级查看作业

```http
GET /api/assignments/class/{classId}?page=1&size=10
Authorization: Bearer <token>
```

### 查看作业详情

```http
GET /api/assignments/{id}
Authorization: Bearer <token>
```

## 3. 提交与批改接口

### 学生上传作业图片

```http
POST /api/submissions/upload
Authorization: Bearer <token>
Content-Type: multipart/form-data
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `assignmentId` | Long | 作业 ID |
| `image` | File | 作业图片，支持 JPG/PNG/WebP |

同一学生对同一作业再次上传时，系统会视为重新提交，覆盖旧图片、清空旧批改结果，并重新执行 OCR 和批改。

### 学生查看自己的提交记录

```http
GET /api/submissions/my?page=1&size=10
Authorization: Bearer <token>
```

### 学生查看错题列表

```http
GET /api/submissions/wrong
Authorization: Bearer <token>
```

返回内容包含作业标题、学科、题号、题干、学生答案、参考答案、得分和反馈。错题范围包括错误和部分正确的题目。

### 教师查看自己班级内的全部提交

```http
GET /api/submissions/teacher?page=1&size=20
Authorization: Bearer <token>
```

教师端“提交”页面使用该接口展示学生上传上来的作业。

### 教师查看某个作业的学生提交

```http
GET /api/submissions/assignment/{assignmentId}?page=1&size=10
Authorization: Bearer <token>
```

### 查看批改详情

```http
GET /api/submissions/{id}/grading
Authorization: Bearer <token>
```

提交状态：

| status | 说明 |
| --- | --- |
| 0 | 待识别 |
| 1 | 识别中 |
| 2 | 识别完成 |
| 3 | 批改中 |
| 4 | 批改完成 |
| 5 | 处理失败 |

批改结果：

| isCorrect | 说明 |
| --- | --- |
| 0 | 错误 |
| 1 | 正确 |
| 2 | 部分正确 |
| null | 待人工批改 |

## 4. 教师批注接口

### 查看某次提交的批注

```http
GET /api/annotations/submission/{submissionId}
Authorization: Bearer <token>
```

### 新增批注

```http
POST /api/annotations
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "submissionId": 1,
  "content": "第 3 题思路基本正确，但关键词不够完整。"
}
```

教师可新增批注，学生可在批改详情页查看。

## 5. 统计接口

### 作业统计

```http
GET /api/statistics/assignment/{assignmentId}
Authorization: Bearer <token>
```

### 学生成绩趋势

```http
GET /api/statistics/student/{studentId}/trend
Authorization: Bearer <token>
```

### 教师班级统计

```http
GET /api/statistics/teacher/classes
Authorization: Bearer <token>
```

返回每个班级的作业数、提交数、已批改数、平均分和及格率。教师端“统计分析”页面使用该接口。

## 6. OCR 服务接口

OCR 服务默认地址：

```text
http://localhost:5000
```

### 健康检查

```http
GET /api/health
```

### OCR 识别

```http
POST /api/ocr/recognize
Content-Type: multipart/form-data
```

| 字段 | 说明 |
| --- | --- |
| `image` | 图片文件 |
| `regions` | 可选，题目区域配置 JSON |

实际批改时，后端不会完全相信 OCR 返回顺序，会根据题号、题干、标准答案和关键词再做一次答案对齐。
