package com.homework.ocr.model

import com.google.gson.annotations.SerializedName

// ===== 通用响应包装 =====
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
) {
    val isSuccess get() = code == 200
}

// ===== 分页数据 =====
data class PageData<T>(
    val records: List<T>,
    val total: Long,
    val current: Long,
    val size: Long
)

// ===== 认证相关 =====
data class LoginRequest(val username: String, val password: String)

data class RegisterRequest(
    val username: String,
    val password: String,
    val realName: String,
    val email: String? = null,
    val phone: String? = null,
    val role: Int  // 1-学生 2-教师
)

data class LoginData(
    val accessToken: String,
    val refreshToken: String,
    val userId: Long,
    val username: String,
    val realName: String,
    val role: Int
)

// ===== 作业相关 =====
data class AssignmentData(
    val id: Long,
    val title: String,
    val description: String?,
    val classId: Long,
    val teacherId: Long,
    val subject: String?,
    val deadline: String?,
    val totalScore: Double,
    val status: Int,
    val createdTime: String?
)

data class AssignmentDetailData(
    val assignment: AssignmentData,
    val questions: List<QuestionData>
)

data class QuestionData(
    val id: Long,
    val assignmentId: Long,
    val questionNo: Int,
    val questionType: Int,  // 1-选择 2-填空 3-简答
    val questionText: String?,
    val answerKey: String,
    val score: Double,
    val gradingMode: Int,
    val keywords: String?
)

// ===== 提交相关 =====
data class SubmissionData(
    val id: Long,
    val assignmentId: Long,
    val studentId: Long,
    val imageUrl: String,
    val status: Int,    // 0-待识别 1-识别中 2-识别完成 3-批改中 4-批改完成 5-失败
    val totalScore: Double?,
    val submitTime: String?,
    val gradedTime: String?
)

data class GradingDetailData(
    val submission: SubmissionData,
    val gradingResults: List<GradingResultData>
)

data class GradingResultData(
    val id: Long,
    val questionNo: Int,
    val studentAnswer: String?,
    val isCorrect: Int?,  // 0-错 1-对 2-部分对
    val scoreGot: Double,
    val scoreFull: Double,
    val feedback: String?,
    val gradingType: Int
)

// ===== 统计相关 =====
data class StatisticsData(
    val totalCount: Long,
    val gradedCount: Long,
    val avgScore: Double?,
    val maxScore: Double?,
    val minScore: Double?,
    val passRate: Double
)
