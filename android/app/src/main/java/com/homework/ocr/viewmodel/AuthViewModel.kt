package com.homework.ocr.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homework.ocr.model.LoginData
import com.homework.ocr.model.RegisterRequest
import com.homework.ocr.repository.HomeworkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: HomeworkRepository
) : ViewModel() {

    private val _loginResult = MutableLiveData<Result<LoginData>>()
    val loginResult: LiveData<Result<LoginData>> = _loginResult

    private val _registerResult = MutableLiveData<Result<LoginData>>()
    val registerResult: LiveData<Result<LoginData>> = _registerResult

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loading.value = true
            _loginResult.value = repository.login(username, password)
            _loading.value = false
        }
    }

    fun register(username: String, password: String, realName: String, role: Int, email: String? = null) {
        viewModelScope.launch {
            _loading.value = true
            _registerResult.value = repository.register(
                RegisterRequest(username, password, realName, email, null, role)
            )
            _loading.value = false
        }
    }

    fun logout() {
        viewModelScope.launch { repository.logout() }
    }
}
