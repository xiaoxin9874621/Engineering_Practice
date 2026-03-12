package com.homework.ocr.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homework.ocr.model.*
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

    // ---- 教师端作业 ----
    private val _teacherAssignments = MutableLiveData<PageData<AssignmentData>>()
    val teacherAssignments: LiveData<PageData<AssignmentData>> = _teacherAssignments

    fun loadTeacherAssignments(page: Int = 1) = viewModelScope.launch {
        _loading.value = true
        repository.getTeacherAssignments(page)
            .onSuccess { _teacherAssignments.value = it }
            .onFailure { _error.value = it.message }
        _loading.value = false
    }

    // ---- 学生端作业 ----
    private val _classAssignments = MutableLiveData<PageData<AssignmentData>>()
    val classAssignments: LiveData<PageData<AssignmentData>> = _classAssignments

    fun loadClassAssignments(classId: Long, page: Int = 1) = viewModelScope.launch {
        _loading.value = true
        repository.getClassAssignments(classId, page)
            .onSuccess { _classAssignments.value = it }
            .onFailure { _error.value = it.message }
        _loading.value = false
    }

    // ---- 作业详情 ----
    private val _assignmentDetail = MutableLiveData<AssignmentDetailData>()
    val assignmentDetail: LiveData<AssignmentDetailData> = _assignmentDetail

    fun loadAssignmentDetail(id: Long) = viewModelScope.launch {
        repository.getAssignmentDetail(id)
            .onSuccess { _assignmentDetail.value = it }
            .onFailure { _error.value = it.message }
    }

    // ---- 提交作业 ----
    private val _uploadResult = MutableLiveData<Result<SubmissionData>>()
    val uploadResult: LiveData<Result<SubmissionData>> = _uploadResult

    fun uploadSubmission(assignmentId: Long, imageFile: File) = viewModelScope.launch {
        _loading.value = true
        _uploadResult.value = repository.uploadSubmission(assignmentId, imageFile)
        _loading.value = false
    }

    // ---- 我的提交 ----
    private val _mySubmissions = MutableLiveData<PageData<SubmissionData>>()
    val mySubmissions: LiveData<PageData<SubmissionData>> = _mySubmissions

    fun loadMySubmissions(page: Int = 1) = viewModelScope.launch {
        _loading.value = true
        repository.getMySubmissions(page)
            .onSuccess { _mySubmissions.value = it }
            .onFailure { _error.value = it.message }
        _loading.value = false
    }

    // ---- 批改详情 ----
    private val _gradingDetail = MutableLiveData<GradingDetailData>()
    val gradingDetail: LiveData<GradingDetailData> = _gradingDetail

    fun loadGradingDetail(submissionId: Long) = viewModelScope.launch {
        repository.getGradingDetail(submissionId)
            .onSuccess { _gradingDetail.value = it }
            .onFailure { _error.value = it.message }
    }

    // ---- 统计 ----
    private val _stats = MutableLiveData<StatisticsData>()
    val stats: LiveData<StatisticsData> = _stats

    fun loadAssignmentStats(assignmentId: Long) = viewModelScope.launch {
        repository.getAssignmentStats(assignmentId)
            .onSuccess { _stats.value = it }
            .onFailure { _error.value = it.message }
    }
}
