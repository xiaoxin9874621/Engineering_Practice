package com.homework.ocr.repository

import com.homework.ocr.model.*
import com.homework.ocr.network.ApiService
import com.homework.ocr.util.UserPreferences
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeworkRepository @Inject constructor(
    private val apiService: ApiService,
    private val userPreferences: UserPreferences
) {
    suspend fun login(username: String, password: String): Result<LoginData> = runCatching {
        val resp = apiService.login(LoginRequest(username, password))
        val body = resp.body() ?: throw Exception("服务器无响应")
        if (!body.isSuccess) throw Exception(body.message)
        val data = body.data!!
        userPreferences.saveLoginInfo(data.accessToken, data.refreshToken, data.userId, data.username, data.realName, data.role)
        data
    }

    suspend fun register(req: RegisterRequest): Result<LoginData> = runCatching {
        val resp = apiService.register(req)
        val body = resp.body() ?: throw Exception("服务器无响应")
        if (!body.isSuccess) throw Exception(body.message)
        val data = body.data!!
        userPreferences.saveLoginInfo(data.accessToken, data.refreshToken, data.userId, data.username, data.realName, data.role)
        data
    }

    suspend fun demoLogin(role: Int): Result<LoginData> = runCatching {
        val data = if (role == 2) {
            LoginData("demo-token", "demo-refresh", 2L, "demo_teacher", "演示教师", 2)
        } else {
            LoginData("demo-token", "demo-refresh", 1L, "demo_student", "演示学生", 1)
        }
        userPreferences.saveLoginInfo(data.accessToken, data.refreshToken, data.userId, data.username, data.realName, data.role)
        data
    }

    suspend fun logout() = userPreferences.clearAll()

    suspend fun getTeacherAssignments(page: Int = 1): Result<PageData<AssignmentData>> = runCatching {
        if (isDemoMode()) return@runCatching demoAssignments()
        apiService.getTeacherAssignments(page).body()?.data ?: throw Exception("获取作业列表失败")
    }

    suspend fun getClassAssignments(classId: Long, page: Int = 1): Result<PageData<AssignmentData>> = runCatching {
        if (isDemoMode()) return@runCatching demoAssignments()
        apiService.getClassAssignments(classId, page).body()?.data ?: throw Exception("获取作业列表失败")
    }

    suspend fun getStudentAssignments(page: Int = 1): Result<PageData<AssignmentData>> = runCatching {
        if (isDemoMode()) return@runCatching demoAssignments()
        apiService.getStudentAssignments(page).body()?.data ?: throw Exception("获取班级作业失败")
    }

    suspend fun getAssignmentDetail(id: Long): Result<AssignmentDetailData> = runCatching {
        if (isDemoMode()) return@runCatching AssignmentDetailData(
            demoAssignments().records.first { it.id == id },
            demoQuestions(id)
        )
        apiService.getAssignmentDetail(id).body()?.data ?: throw Exception("获取作业详情失败")
    }

    suspend fun createAssignment(
        title: String,
        subject: String,
        classId: Long,
        questions: List<CreateQuestionRequest>
    ): Result<AssignmentData> = runCatching {
        if (questions.isEmpty()) throw Exception("请至少添加一道题")
        if (isDemoMode()) {
            return@runCatching AssignmentData(99L, title, "演示作业", classId, 2L, subject, null, questions.sumOf { it.score }, 1, "2026-06-16 12:00")
        }
        val body = CreateAssignmentRequest(title, "由 Android 客户端创建", classId, subject, questions)
        val resp = apiService.createAssignment(body)
        val response = resp.body() ?: throw Exception("创建作业失败")
        if (!response.isSuccess) throw Exception(response.message)
        response.data!!
    }

    suspend fun uploadSubmission(assignmentId: Long, imageFile: File): Result<SubmissionData> = runCatching {
        if (isDemoMode()) {
            return@runCatching demoSubmissions(assignmentId).records.first()
        }
        val assignmentIdBody = assignmentId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val imageRequestBody = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, imageRequestBody)
        val body = apiService.uploadSubmission(assignmentIdBody, imagePart).body() ?: throw Exception("上传失败")
        if (!body.isSuccess) throw Exception(body.message)
        body.data!!
    }

    suspend fun getMySubmissions(page: Int = 1): Result<PageData<SubmissionData>> = runCatching {
        if (isDemoMode()) return@runCatching demoSubmissions()
        apiService.getMySubmissions(page).body()?.data ?: throw Exception("获取提交记录失败")
    }

    suspend fun getTeacherSubmissions(page: Int = 1): Result<PageData<SubmissionData>> = runCatching {
        if (isDemoMode()) return@runCatching demoSubmissions()
        apiService.getTeacherSubmissions(page).body()?.data ?: throw Exception("获取班级提交记录失败")
    }

    suspend fun getAssignmentSubmissions(assignmentId: Long, page: Int = 1): Result<PageData<SubmissionData>> = runCatching {
        if (isDemoMode()) return@runCatching demoSubmissions(assignmentId)
        apiService.getAssignmentSubmissions(assignmentId, page).body()?.data ?: throw Exception("获取提交列表失败")
    }

    suspend fun getWrongQuestions(): Result<List<WrongQuestionData>> = runCatching {
        if (isDemoMode()) return@runCatching demoWrongQuestions()
        apiService.getWrongQuestions().body()?.data ?: emptyList()
    }

    suspend fun getGradingDetail(submissionId: Long): Result<GradingDetailData> = runCatching {
        if (isDemoMode()) return@runCatching GradingDetailData(
            submission = demoSubmissions().records.first(),
            gradingResults = listOf(
                GradingResultData(1L, 1, "A", 1, 10.0, 10.0, "回答正确", 1),
                GradingResultData(2L, 2, "光合作用", 1, 20.0, 20.0, "回答正确", 1),
                GradingResultData(3L, 3, "两点可以确定一条直线", 2, 50.0, 70.0, "包含 2/3 个关键词", 1)
            )
        )
        apiService.getGradingDetail(submissionId).body()?.data ?: throw Exception("获取批改详情失败")
    }

    suspend fun getAnnotations(submissionId: Long): Result<List<AnnotationData>> = runCatching {
        if (isDemoMode()) return@runCatching demoAnnotations(submissionId)
        apiService.getAnnotations(submissionId).body()?.data ?: emptyList()
    }

    suspend fun createAnnotation(submissionId: Long, content: String): Result<AnnotationData> = runCatching {
        if (isDemoMode()) return@runCatching AnnotationData(System.currentTimeMillis(), submissionId, 2L, content, null, null, "#FF0000", "2026-06-16 12:00")
        val body = apiService.createAnnotation(CreateAnnotationRequest(submissionId, content)).body() ?: throw Exception("保存批注失败")
        if (!body.isSuccess) throw Exception(body.message)
        body.data!!
    }

    suspend fun getAssignmentStats(assignmentId: Long): Result<StatisticsData> = runCatching {
        if (isDemoMode()) return@runCatching StatisticsData(42, 39, 86.5, 100.0, 58.0, 94.8)
        apiService.getAssignmentStats(assignmentId).body()?.data ?: throw Exception("获取统计数据失败")
    }

    suspend fun getTeacherClassStats(): Result<List<ClassStatsData>> = runCatching {
        if (isDemoMode()) return@runCatching listOf(ClassStatsData(1L, "一班", 2, 2, 1, 92.0, 100.0))
        apiService.getTeacherClassStats().body()?.data ?: emptyList()
    }

    suspend fun getStudentTrend(studentId: Long): Result<List<SubmissionData>> = runCatching {
        if (isDemoMode()) return@runCatching demoSubmissions().records
        apiService.getStudentTrend(studentId).body()?.data ?: emptyList()
    }

    private suspend fun isDemoMode(): Boolean = userPreferences.accessToken.first() == "demo-token"

    private fun demoAssignments(): PageData<AssignmentData> {
        val records = listOf(
            AssignmentData(1L, "数学周测：一次函数", "完成 1-3 题并拍照上传", 1L, 2L, "数学", "2026-06-20 18:00", 100.0, 1, "2026-06-15 09:00"),
            AssignmentData(2L, "语文阅读理解训练", "圈画关键词并完成简答题", 1L, 2L, "语文", "2026-06-21 20:00", 100.0, 1, "2026-06-15 14:00")
        )
        return PageData(records, records.size.toLong(), 1, records.size.toLong())
    }

    private fun demoQuestions(assignmentId: Long): List<QuestionData> = listOf(
        QuestionData(1L, assignmentId, 1, 1, "选择正确答案", "A", 10.0, 1, null),
        QuestionData(2L, assignmentId, 2, 2, "填写关键概念", "光合作用", 20.0, 1, null),
        QuestionData(3L, assignmentId, 3, 3, "简述一次函数图像特点", "经过两点确定一条直线", 70.0, 2, "[\"两点\",\"直线\",\"函数\"]")
    )

    private fun demoSubmissions(assignmentId: Long? = null): PageData<SubmissionData> {
        val records = listOf(
            SubmissionData(101L, assignmentId ?: 1L, 3L, "/uploads/demo-1.jpg", 4, 92.0, "2026-06-16 10:30", "2026-06-16 10:31", "数学周测：一次函数", "数学"),
            SubmissionData(102L, assignmentId ?: 1L, 4L, "/uploads/demo-2.jpg", 3, null, "2026-06-16 11:10", null, "数学周测：一次函数", "数学")
        )
        return PageData(records, records.size.toLong(), 1, records.size.toLong())
    }

    private fun demoWrongQuestions(): List<WrongQuestionData> = listOf(
        WrongQuestionData(1L, 101L, 3L, 3, "两点可以确定直线", 2, 50.0, 70.0, "缺少关键词：函数", 1, "2026-06-16 10:31", 1L, "数学周测：一次函数", "数学", "简述一次函数图像特点", "经过两点确定一条直线")
    )

    private fun demoAnnotations(submissionId: Long): List<AnnotationData> = listOf(
        AnnotationData(1L, submissionId, 2L, "第 3 题思路基本正确，但需要把函数图像特点写完整。", null, null, "#FF0000", "2026-06-16 11:20")
    )
}
