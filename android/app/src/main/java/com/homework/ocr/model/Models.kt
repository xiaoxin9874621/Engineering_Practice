package com.homework.ocr.model

data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
) {
    val isSuccess get() = code == 200
}

data class PageData<T>(
    val records: List<T>,
    val total: Long,
    val current: Long,
    val size: Long
)

data class LoginRequest(val username: String, val password: String)

data class RegisterRequest(
    val username: String,
    val password: String,
    val realName: String,
    val email: String? = null,
    val phone: String? = null,
    val role: Int
)

data class LoginData(
    val accessToken: String,
    val refreshToken: String,
    val userId: Long,
    val username: String,
    val realName: String,
    val role: Int
)

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
    val questionType: Int,
    val questionText: String?,
    val answerKey: String,
    val score: Double,
    val gradingMode: Int,
    val keywords: String?
)

data class CreateAssignmentRequest(
    val title: String,
    val description: String?,
    val classId: Long,
    val subject: String,
    val questions: List<CreateQuestionRequest>
)

data class CreateQuestionRequest(
    val questionNo: Int,
    val questionType: Int,
    val questionText: String,
    val answerKey: String,
    val score: Double,
    val gradingMode: Int,
    val keywords: String?
)

data class SubmissionData(
    val id: Long,
    val assignmentId: Long,
    val studentId: Long,
    val imageUrl: String,
    val status: Int,
    val totalScore: Double?,
    val submitTime: String?,
    val gradedTime: String?,
    val assignmentTitle: String? = null,
    val subject: String? = null
)

data class GradingDetailData(
    val submission: SubmissionData,
    val gradingResults: List<GradingResultData>
)

data class GradingResultData(
    val id: Long,
    val questionNo: Int,
    val studentAnswer: String?,
    val isCorrect: Int?,
    val scoreGot: Double,
    val scoreFull: Double,
    val feedback: String?,
    val gradingType: Int
)

data class AnnotationData(
    val id: Long,
    val submissionId: Long,
    val teacherId: Long,
    val content: String,
    val positionX: Int?,
    val positionY: Int?,
    val color: String?,
    val createdTime: String?
)

data class CreateAnnotationRequest(
    val submissionId: Long,
    val content: String,
    val positionX: Int? = null,
    val positionY: Int? = null,
    val color: String = "#FF0000"
)

data class StatisticsData(
    val totalCount: Long,
    val gradedCount: Long,
    val avgScore: Double?,
    val maxScore: Double?,
    val minScore: Double?,
    val passRate: Double
)

data class ClassStatsData(
    val classId: Long,
    val className: String,
    val assignmentCount: Int,
    val submissionCount: Int,
    val gradedCount: Int,
    val avgScore: Double,
    val passRate: Double
)

data class WrongQuestionData(
    val id: Long,
    val submissionId: Long,
    val questionId: Long,
    val questionNo: Int,
    val studentAnswer: String?,
    val isCorrect: Int?,
    val scoreGot: Double?,
    val scoreFull: Double?,
    val feedback: String?,
    val gradingType: Int?,
    val createdTime: String?,
    val assignmentId: Long,
    val assignmentTitle: String?,
    val subject: String?,
    val questionText: String?,
    val answerKey: String?
)
