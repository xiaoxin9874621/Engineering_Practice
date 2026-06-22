package com.homework.ocr.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homework.ocr.model.AssignmentData
import com.homework.ocr.model.AssignmentDetailData
import com.homework.ocr.model.AnnotationData
import com.homework.ocr.model.ClassStatsData
import com.homework.ocr.model.CreateQuestionRequest
import com.homework.ocr.model.GradingDetailData
import com.homework.ocr.model.PageData
import com.homework.ocr.model.StatisticsData
import com.homework.ocr.model.SubmissionData
import com.homework.ocr.model.WrongQuestionData
import com.homework.ocr.repository.HomeworkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class HomeworkViewModel @Inject constructor(
    private val repository: HomeworkRepository
) : ViewModel() {

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _teacherAssignments = MutableLiveData<PageData<AssignmentData>>()
    val teacherAssignments: LiveData<PageData<AssignmentData>> = _teacherAssignments

    fun loadTeacherAssignments(page: Int = 1) = viewModelScope.launch {
        _loading.value = true
        repository.getTeacherAssignments(page)
            .onSuccess { _teacherAssignments.value = it }
            .onFailure { _error.value = it.message }
        _loading.value = false
    }

    private val _classAssignments = MutableLiveData<PageData<AssignmentData>>()
    val classAssignments: LiveData<PageData<AssignmentData>> = _classAssignments

    fun loadClassAssignments(classId: Long, page: Int = 1) = viewModelScope.launch {
        _loading.value = true
        repository.getClassAssignments(classId, page)
            .onSuccess { _classAssignments.value = it }
            .onFailure { _error.value = it.message }
        _loading.value = false
    }

    fun loadStudentAssignments(page: Int = 1) = viewModelScope.launch {
        _loading.value = true
        repository.getStudentAssignments(page)
            .onSuccess { _classAssignments.value = it }
            .onFailure { _error.value = it.message }
        _loading.value = false
    }

    private val _assignmentDetail = MutableLiveData<AssignmentDetailData>()
    val assignmentDetail: LiveData<AssignmentDetailData> = _assignmentDetail

    fun loadAssignmentDetail(id: Long) = viewModelScope.launch {
        repository.getAssignmentDetail(id)
            .onSuccess { _assignmentDetail.value = it }
            .onFailure { _error.value = it.message }
    }

    private val _createAssignmentResult = MutableLiveData<Result<AssignmentData>>()
    val createAssignmentResult: LiveData<Result<AssignmentData>> = _createAssignmentResult

    fun createAssignment(
        title: String,
        subject: String,
        classId: Long,
        questions: List<CreateQuestionRequest>
    ) = viewModelScope.launch {
        _loading.value = true
        _createAssignmentResult.value = repository.createAssignment(title, subject, classId, questions)
        _loading.value = false
    }

    private val _uploadResult = MutableLiveData<Result<SubmissionData>>()
    val uploadResult: LiveData<Result<SubmissionData>> = _uploadResult

    fun uploadSubmission(assignmentId: Long, imageFile: File) = viewModelScope.launch {
        _loading.value = true
        _uploadResult.value = repository.uploadSubmission(assignmentId, imageFile)
        _loading.value = false
    }

    private val _mySubmissions = MutableLiveData<PageData<SubmissionData>>()
    val mySubmissions: LiveData<PageData<SubmissionData>> = _mySubmissions

    fun loadMySubmissions(page: Int = 1) = viewModelScope.launch {
        _loading.value = true
        repository.getMySubmissions(page)
            .onSuccess { _mySubmissions.value = it }
            .onFailure { _error.value = it.message }
        _loading.value = false
    }

    private val _assignmentSubmissions = MutableLiveData<PageData<SubmissionData>>()
    val assignmentSubmissions: LiveData<PageData<SubmissionData>> = _assignmentSubmissions

    fun loadAssignmentSubmissions(assignmentId: Long, page: Int = 1) = viewModelScope.launch {
        _loading.value = true
        repository.getAssignmentSubmissions(assignmentId, page)
            .onSuccess { _assignmentSubmissions.value = it }
            .onFailure { _error.value = it.message }
        _loading.value = false
    }

    fun loadTeacherSubmissions(page: Int = 1) = viewModelScope.launch {
        _loading.value = true
        repository.getTeacherSubmissions(page)
            .onSuccess { _assignmentSubmissions.value = it }
            .onFailure { _error.value = it.message }
        _loading.value = false
    }

    private val _wrongQuestions = MutableLiveData<List<WrongQuestionData>>()
    val wrongQuestions: LiveData<List<WrongQuestionData>> = _wrongQuestions

    fun loadWrongQuestions() = viewModelScope.launch {
        _loading.value = true
        repository.getWrongQuestions()
            .onSuccess { _wrongQuestions.value = it }
            .onFailure { _error.value = it.message }
        _loading.value = false
    }

    private val _gradingDetail = MutableLiveData<GradingDetailData>()
    val gradingDetail: LiveData<GradingDetailData> = _gradingDetail

    fun loadGradingDetail(submissionId: Long) = viewModelScope.launch {
        _loading.value = true
        repository.getGradingDetail(submissionId)
            .onSuccess { _gradingDetail.value = it }
            .onFailure { _error.value = it.message }
        _loading.value = false
    }

    private val _annotations = MutableLiveData<List<AnnotationData>>()
    val annotations: LiveData<List<AnnotationData>> = _annotations

    private val _createAnnotationResult = MutableLiveData<Result<AnnotationData>>()
    val createAnnotationResult: LiveData<Result<AnnotationData>> = _createAnnotationResult

    fun loadAnnotations(submissionId: Long) = viewModelScope.launch {
        repository.getAnnotations(submissionId)
            .onSuccess { _annotations.value = it }
            .onFailure { _error.value = it.message }
    }

    fun createAnnotation(submissionId: Long, content: String) = viewModelScope.launch {
        _loading.value = true
        val result = repository.createAnnotation(submissionId, content)
        _createAnnotationResult.value = result
        result.onSuccess { loadAnnotations(submissionId) }
            .onFailure { _error.value = it.message }
        _loading.value = false
    }

    private val _stats = MutableLiveData<StatisticsData>()
    val stats: LiveData<StatisticsData> = _stats

    private val _classStats = MutableLiveData<List<ClassStatsData>>()
    val classStats: LiveData<List<ClassStatsData>> = _classStats

    fun loadAssignmentStats(assignmentId: Long) = viewModelScope.launch {
        repository.getAssignmentStats(assignmentId)
            .onSuccess { _stats.value = it }
            .onFailure { _error.value = it.message }
    }

    fun loadTeacherClassStats() = viewModelScope.launch {
        repository.getTeacherClassStats()
            .onSuccess { _classStats.value = it }
            .onFailure { _error.value = it.message }
    }
}
