package com.homework.ocr.repository

import com.homework.ocr.model.*
import com.homework.ocr.network.ApiService
import com.homework.ocr.util.UserPreferences
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
    // ---- 认证 ----

    suspend fun login(username: String, password: String): Result<LoginData> = runCatching {
        val resp = apiService.login(LoginRequest(username, password))
        val body = resp.body() ?: throw Exception("服务器无响应")
        if (!body.isSuccess) throw Exception(body.message)
        val data = body.data!!
        userPreferences.saveLoginInfo(
            data.accessToken, data.refreshToken,
            data.userId, data.username, data.realName, data.role
        )
        data
    }

    suspend fun register(req: RegisterRequest): Result<LoginData> = runCatching {
        val resp = apiService.register(req)
        val body = resp.body() ?: throw Exception("服务器无响应")
        if (!body.isSuccess) throw Exception(body.message)
        val data = body.data!!
        userPreferences.saveLoginInfo(
            data.accessToken, data.refreshToken,
            data.userId, data.username, data.realName, data.role
        )
        data
    }

    suspend fun logout() = userPreferences.clearAll()

    // ---- 作业 ----

    suspend fun getTeacherAssignments(page: Int = 1): Result<PageData<AssignmentData>> = runCatching {
        val resp = apiService.getTeacherAssignments(page)
        resp.body()?.data ?: throw Exception("获取作业列表失败")
    }

    suspend fun getClassAssignments(classId: Long, page: Int = 1): Result<PageData<AssignmentData>> = runCatching {
        val resp = apiService.getClassAssignments(classId, page)
        resp.body()?.data ?: throw Exception("获取作业列表失败")
    }

    suspend fun getAssignmentDetail(id: Long): Result<AssignmentDetailData> = runCatching {
        val resp = apiService.getAssignmentDetail(id)
        resp.body()?.data ?: throw Exception("获取作业详情失败")
    }

    // ---- 提交 ----

    suspend fun uploadSubmission(assignmentId: Long, imageFile: File): Result<SubmissionData> = runCatching {
        val assignmentIdBody = assignmentId.toString()
            .toRequestBody("text/plain".toMediaTypeOrNull())
        val imageRequestBody = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, imageRequestBody)
        val resp = apiService.uploadSubmission(assignmentIdBody, imagePart)
        val body = resp.body() ?: throw Exception("上传失败")
        if (!body.isSuccess) throw Exception(body.message)
        body.data!!
    }

    suspend fun getMySubmissions(page: Int = 1): Result<PageData<SubmissionData>> = runCatching {
        val resp = apiService.getMySubmissions(page)
        resp.body()?.data ?: throw Exception("获取提交记录失败")
    }

    suspend fun getAssignmentSubmissions(assignmentId: Long, page: Int = 1): Result<PageData<SubmissionData>> = runCatching {
        val resp = apiService.getAssignmentSubmissions(assignmentId, page)
        resp.body()?.data ?: throw Exception("获取提交列表失败")
    }

    suspend fun getGradingDetail(submissionId: Long): Result<GradingDetailData> = runCatching {
        val resp = apiService.getGradingDetail(submissionId)
        resp.body()?.data ?: throw Exception("获取批改详情失败")
    }

    // ---- 统计 ----

    suspend fun getAssignmentStats(assignmentId: Long): Result<StatisticsData> = runCatching {
        val resp = apiService.getAssignmentStats(assignmentId)
        resp.body()?.data ?: throw Exception("获取统计数据失败")
    }

    suspend fun getStudentTrend(studentId: Long): Result<List<SubmissionData>> = runCatching {
        val resp = apiService.getStudentTrend(studentId)
        resp.body()?.data ?: emptyList()
    }
}
