package com.homework.ocr.ui.teacher

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.homework.ocr.R
import com.homework.ocr.databinding.ActivityTeacherMainBinding
import com.homework.ocr.ui.auth.LoginActivity
import com.homework.ocr.util.UserPreferences
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

        lifecycleScope.launch {
            val realName = userPreferences.realName.first().orEmpty()
            binding.tvTeacherName.text = realName
            binding.toolbar.subtitle = "教师：$realName"
        }

        setupBottomNav()
        setupLogout()
        loadFragment(TeacherAssignmentsFragment())
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.tab_assignments -> loadFragment(TeacherAssignmentsFragment())
                R.id.tab_submissions -> loadFragment(TeacherSubmissionsFragment())
                R.id.tab_statistics -> loadFragment(StatisticsFragment())
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
            .replace(R.id.viewPager, fragment)
            .commit()
    }
}
