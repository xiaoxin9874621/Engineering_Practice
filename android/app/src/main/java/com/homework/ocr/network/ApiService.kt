package com.homework.ocr.network

import com.homework.ocr.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ==================== 认证 ====================

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginData>>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<LoginData>>

    // ==================== 作业管理 ====================

    @GET("api/assignments/teacher")
    suspend fun getTeacherAssignments(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<PageData<AssignmentData>>>

    @GET("api/assignments/class/{classId}")
    suspend fun getClassAssignments(
        @Path("classId") classId: Long,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<PageData<AssignmentData>>>

    @GET("api/assignments/{id}")
    suspend fun getAssignmentDetail(@Path("id") id: Long): Response<ApiResponse<AssignmentDetailData>>

    @POST("api/assignments")
    suspend fun createAssignment(@Body body: Map<String, Any>): Response<ApiResponse<AssignmentData>>

    @PUT("api/assignments/{id}/status")
    suspend fun updateAssignmentStatus(
        @Path("id") id: Long,
        @Query("status") status: Int
    ): Response<ApiResponse<Void>>

    // ==================== 提交管理 ====================

    @Multipart
    @POST("api/submissions/upload")
    suspend fun uploadSubmission(
        @Part("assignmentId") assignmentId: RequestBody,
        @Part image: MultipartBody.Part
    ): Response<ApiResponse<SubmissionData>>

    @GET("api/submissions/my")
    suspend fun getMySubmissions(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<PageData<SubmissionData>>>

    @GET("api/submissions/assignment/{assignmentId}")
    suspend fun getAssignmentSubmissions(
        @Path("assignmentId") assignmentId: Long,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<PageData<SubmissionData>>>

    @GET("api/submissions/{id}/grading")
    suspend fun getGradingDetail(@Path("id") id: Long): Response<ApiResponse<GradingDetailData>>

    // ==================== 统计分析 ====================

    @GET("api/statistics/assignment/{assignmentId}")
    suspend fun getAssignmentStats(@Path("assignmentId") assignmentId: Long): Response<ApiResponse<StatisticsData>>

    @GET("api/statistics/student/{studentId}/trend")
    suspend fun getStudentTrend(@Path("studentId") studentId: Long): Response<ApiResponse<List<SubmissionData>>>
}
