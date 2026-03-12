package com.homework.ocr.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.homework.ocr.databinding.ActivityRegisterBinding
import com.homework.ocr.ui.teacher.TeacherMainActivity
import com.homework.ocr.ui.student.StudentMainActivity
import com.homework.ocr.viewmodel.AuthViewModel
import com.homework.ocr.model.LoginData
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.loading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnRegister.isEnabled = !loading
        }
        viewModel.registerResult.observe(this) { result ->
            result.onSuccess { navigateToMain(it) }
                  .onFailure { Toast.makeText(this, it.message, Toast.LENGTH_LONG).show() }
        }
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            val realName = binding.etRealName.text.toString().trim()
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val email = binding.etEmail.text.toString().trim().takeIf { it.isNotEmpty() }

            if (realName.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "请填写姓名、用户名和密码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(this, "密码长度不能少于6位", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val role = if (binding.rbTeacher.isChecked) 2 else 1
            viewModel.register(username, password, realName, role, email)
        }
        binding.tvLogin.setOnClickListener { finish() }
    }

    private fun navigateToMain(data: LoginData) {
        val intent = when (data.role) {
            2, 3 -> Intent(this, TeacherMainActivity::class.java)
            else -> Intent(this, StudentMainActivity::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
