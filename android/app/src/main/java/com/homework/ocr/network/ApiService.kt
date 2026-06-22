package com.homework.ocr.network

import com.homework.ocr.model.AnnotationData
import com.homework.ocr.model.ApiResponse
import com.homework.ocr.model.AssignmentData
import com.homework.ocr.model.AssignmentDetailData
import com.homework.ocr.model.CreateAnnotationRequest
import com.homework.ocr.model.CreateAssignmentRequest
import com.homework.ocr.model.ClassStatsData
import com.homework.ocr.model.GradingDetailData
import com.homework.ocr.model.LoginData
import com.homework.ocr.model.LoginRequest
import com.homework.ocr.model.PageData
import com.homework.ocr.model.RegisterRequest
import com.homework.ocr.model.StatisticsData
import com.homework.ocr.model.SubmissionData
import com.homework.ocr.model.WrongQuestionData
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginData>>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<LoginData>>

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

    @GET("api/assignments/student")
    suspend fun getStudentAssignments(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<PageData<AssignmentData>>>

    @GET("api/assignments/{id}")
    suspend fun getAssignmentDetail(@Path("id") id: Long): Response<ApiResponse<AssignmentDetailData>>

    @POST("api/assignments")
    suspend fun createAssignment(@Body body: CreateAssignmentRequest): Response<ApiResponse<AssignmentData>>

    @PUT("api/assignments/{id}/status")
    suspend fun updateAssignmentStatus(
        @Path("id") id: Long,
        @Query("status") status: Int
    ): Response<ApiResponse<Void>>

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

    @GET("api/submissions/wrong")
    suspend fun getWrongQuestions(): Response<ApiResponse<List<WrongQuestionData>>>

    @GET("api/submissions/assignment/{assignmentId}")
    suspend fun getAssignmentSubmissions(
        @Path("assignmentId") assignmentId: Long,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<PageData<SubmissionData>>>

    @GET("api/submissions/teacher")
    suspend fun getTeacherSubmissions(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<PageData<SubmissionData>>>

    @GET("api/submissions/{id}/grading")
    suspend fun getGradingDetail(@Path("id") id: Long): Response<ApiResponse<GradingDetailData>>

    @GET("api/annotations/submission/{submissionId}")
    suspend fun getAnnotations(@Path("submissionId") submissionId: Long): Response<ApiResponse<List<AnnotationData>>>

    @POST("api/annotations")
    suspend fun createAnnotation(@Body body: CreateAnnotationRequest): Response<ApiResponse<AnnotationData>>

    @GET("api/statistics/assignment/{assignmentId}")
    suspend fun getAssignmentStats(@Path("assignmentId") assignmentId: Long): Response<ApiResponse<StatisticsData>>

    @GET("api/statistics/teacher/classes")
    suspend fun getTeacherClassStats(): Response<ApiResponse<List<ClassStatsData>>>

    @GET("api/statistics/student/{studentId}/trend")
    suspend fun getStudentTrend(@Path("studentId") studentId: Long): Response<ApiResponse<List<SubmissionData>>>
}
