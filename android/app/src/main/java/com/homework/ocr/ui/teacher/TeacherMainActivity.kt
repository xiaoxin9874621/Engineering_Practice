package com.homework.ocr.ui.teacher

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.homework.ocr.databinding.ActivityTeacherMainBinding
import com.homework.ocr.util.UserPreferences
import com.homework.ocr.ui.auth.LoginActivity
import com.homework.ocr.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TeacherMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTeacherMainBinding
    private val authViewModel: AuthViewModel by viewModels()

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeacherMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 加载用户名
        lifecycleScope.launch {
            val realName = userPreferences.realName.first()
            binding.tvTeacherName = binding.root.context // workaround
            binding.toolbar.subtitle = "教师：${realName ?: ""}"
        }

        setupBottomNav()
        setupLogout()

        // 默认显示作业列表
        loadFragment(TeacherAssignmentsFragment())
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                com.homework.ocr.R.id.tab_assignments -> loadFragment(TeacherAssignmentsFragment())
                com.homework.ocr.R.id.tab_submissions -> loadFragment(TeacherSubmissionsFragment())
                com.homework.ocr.R.id.tab_statistics  -> loadFragment(StatisticsFragment())
                else -> return@setOnItemSelectedListener false
            }
            true
        }
    }

    private fun setupLogout() {
        binding.btnLogout.setOnClickListener {
            authViewModel.logout()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(com.homework.ocr.R.id.viewPager, fragment)
            .commit()
    }
}
