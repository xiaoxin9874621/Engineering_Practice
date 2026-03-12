package com.homework.ocr.ui.student

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.homework.ocr.databinding.ActivityStudentMainBinding
import com.homework.ocr.databinding.FragmentStudentHomeworkBinding
import com.homework.ocr.databinding.FragmentSubmissionsBinding
import com.homework.ocr.databinding.ItemAssignmentBinding
import com.homework.ocr.databinding.ItemSubmissionBinding
import com.homework.ocr.model.AssignmentData
import com.homework.ocr.model.SubmissionData
import com.homework.ocr.ui.auth.LoginActivity
import com.homework.ocr.ui.common.GradingDetailActivity
import com.homework.ocr.util.UserPreferences
import com.homework.ocr.viewmodel.AuthViewModel
import com.homework.ocr.viewmodel.HomeworkViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

// ==================== StudentMainActivity ====================
@AndroidEntryPoint
class StudentMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentMainBinding
    private val authViewModel: AuthViewModel by viewModels()

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            val realName = userPreferences.realName.first()
            binding.toolbar.subtitle = "同学：${realName ?: ""}"
        }

        setupBottomNav()
        setupLogout()

        // 默认显示作业列表
        loadFragment(StudentHomeworkFragment())
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                com.homework.ocr.R.id.tab_homework    -> loadFragment(StudentHomeworkFragment())
                com.homework.ocr.R.id.tab_submissions -> loadFragment(StudentSubmissionsFragment())
                com.homework.ocr.R.id.tab_profile     -> loadFragment(ProfileFragment())
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

// ==================== StudentHomeworkFragment ====================
@AndroidEntryPoint
class StudentHomeworkFragment : Fragment() {

    private var _binding: FragmentStudentHomeworkBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeworkViewModel by viewModels()

    // 假设学生 classId = 1（实际应从 UserPreferences 读取）
    private val classId: Long = 1L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStudentHomeworkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = StudentAssignmentAdapter(emptyList()) { assignment ->
            val intent = Intent(requireContext(), UploadHomeworkActivity::class.java).apply {
                putExtra("assignmentId", assignment.id)
                putExtra("assignmentTitle", assignment.title)
            }
            startActivity(intent)
        }
        binding.rvAssignments.adapter = adapter

        viewModel.classAssignments.observe(viewLifecycleOwner) { page ->
            adapter.update(page.records)
            val pending = page.records.count { it.status == 1 }
            binding.tvPendingCount.text = "$pending 项"
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadClassAssignments(classId)
            binding.swipeRefresh.isRefreshing = false
        }

        viewModel.loadClassAssignments(classId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ==================== StudentSubmissionsFragment ====================
@AndroidEntryPoint
class StudentSubmissionsFragment : Fragment() {

    private var _binding: FragmentSubmissionsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeworkViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSubmissionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = SubmissionAdapter(emptyList()) { submission ->
            if (submission.status == 4) {
                val intent = Intent(requireContext(), GradingDetailActivity::class.java)
                intent.putExtra("submissionId", submission.id)
                startActivity(intent)
            } else {
                Toast.makeText(context, "批改还未完成，请稍候", Toast.LENGTH_SHORT).show()
            }
        }
        binding.rvSubmissions.adapter = adapter

        viewModel.mySubmissions.observe(viewLifecycleOwner) { page ->
            adapter.update(page.records)
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadMySubmissions()
            binding.swipeRefresh.isRefreshing = false
        }

        viewModel.loadMySubmissions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ==================== ProfileFragment ====================
class ProfileFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return android.widget.TextView(context).apply {
            text = "个人信息页（建设中）"
            textSize = 16f
            setPadding(48, 48, 48, 48)
        }
    }
}

// ==================== StudentAssignmentAdapter ====================
class StudentAssignmentAdapter(
    private var items: List<AssignmentData>,
    private val onClick: (AssignmentData) -> Unit
) : RecyclerView.Adapter<StudentAssignmentAdapter.VH>() {

    inner class VH(val binding: ItemAssignmentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemAssignmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        with(holder.binding) {
            tvAssignmentTitle.text = item.title
            tvSubject.text = item.subject ?: "-"
            tvTotalScore.text = "满分 ${item.totalScore.toInt()} 分"
            tvDeadline.text = item.deadline?.let { "截止：$it" } ?: "无截止时间"
            tvAssignmentStatus.text = when (item.status) {
                1 -> "已发布"; 2 -> "已结束"; else -> "草稿"
            }
        }
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
    fun update(newItems: List<AssignmentData>) { items = newItems; notifyDataSetChanged() }
}

// ==================== SubmissionAdapter ====================
class SubmissionAdapter(
    private var items: List<SubmissionData>,
    private val onClick: (SubmissionData) -> Unit
) : RecyclerView.Adapter<SubmissionAdapter.VH>() {

    inner class VH(val binding: ItemSubmissionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemSubmissionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        with(holder.binding) {
            tvAssignmentTitle.text = "作业 #${item.assignmentId}"
            tvSubmitTime.text = item.submitTime ?: ""
            tvStatus.text = when (item.status) {
                0 -> "⏳ 等待识别"; 1 -> "🔍 识别中"; 2 -> "✅ 识别完成"
                3 -> "📝 批改中"; 4 -> "🎉 批改完成"; 5 -> "❌ 失败"
                else -> "未知"
            }
            tvScore.text = if (item.status == 4) "${item.totalScore?.toInt() ?: "-"}" else "—"
        }
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
    fun update(newItems: List<SubmissionData>) { items = newItems; notifyDataSetChanged() }
}
