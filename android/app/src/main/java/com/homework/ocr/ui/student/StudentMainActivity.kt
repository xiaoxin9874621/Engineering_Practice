package com.homework.ocr.ui.student

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.homework.ocr.R
import com.homework.ocr.databinding.ActivityStudentMainBinding
import com.homework.ocr.databinding.FragmentStudentHomeworkBinding
import com.homework.ocr.databinding.FragmentSubmissionsBinding
import com.homework.ocr.databinding.ItemAssignmentBinding
import com.homework.ocr.databinding.ItemSubmissionBinding
import com.homework.ocr.model.AssignmentData
import com.homework.ocr.model.SubmissionData
import com.homework.ocr.model.WrongQuestionData
import com.homework.ocr.ui.auth.LoginActivity
import com.homework.ocr.ui.common.GradingDetailActivity
import com.homework.ocr.util.UserPreferences
import com.homework.ocr.viewmodel.AuthViewModel
import com.homework.ocr.viewmodel.HomeworkViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

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
            val realName = userPreferences.realName.first().orEmpty()
            binding.tvStudentName.text = realName
            binding.toolbar.subtitle = "同学：$realName"
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.tab_homework -> loadFragment(StudentHomeworkFragment())
                R.id.tab_submissions -> loadFragment(StudentSubmissionsFragment())
                R.id.tab_profile -> loadFragment(ProfileFragment())
                else -> return@setOnItemSelectedListener false
            }
            true
        }

        binding.btnLogout.setOnClickListener {
            authViewModel.logout()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }

        loadFragment(StudentHomeworkFragment())
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.viewPager, fragment)
            .commit()
    }
}

@AndroidEntryPoint
class StudentHomeworkFragment : Fragment() {

    private var _binding: FragmentStudentHomeworkBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeworkViewModel by viewModels()
    private var assignments: List<AssignmentData> = emptyList()
    private var submissions: List<SubmissionData> = emptyList()

    private val adapter = StudentAssignmentAdapter(emptyList()) { assignment ->
        openUpload(assignment.id, assignment.title, false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStudentHomeworkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvAssignments.adapter = adapter

        viewModel.classAssignments.observe(viewLifecycleOwner) { page ->
            assignments = page.records
            updatePendingList()
        }
        viewModel.mySubmissions.observe(viewLifecycleOwner) { page ->
            submissions = page.records
            updatePendingList()
        }
        viewModel.error.observe(viewLifecycleOwner) { err ->
            if (!err.isNullOrBlank()) Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
        }

        binding.swipeRefresh.setOnRefreshListener {
            reload()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        viewModel.loadStudentAssignments()
        viewModel.loadMySubmissions()
    }

    private fun updatePendingList() {
        val submittedAssignmentIds = submissions.map { it.assignmentId }.toSet()
        val pendingAssignments = assignments.filter { it.status == 1 && it.id !in submittedAssignmentIds }
        adapter.update(pendingAssignments)
        binding.tvPendingCount.text = "${pendingAssignments.size} 项"
        binding.tvPendingHint.text = if (pendingAssignments.isEmpty()) {
            "当前没有待完成作业，已提交内容可在提交记录中查看或重新提交"
        } else {
            "提交成功后会自动从这里移除"
        }
    }

    private fun openUpload(assignmentId: Long, title: String, isResubmit: Boolean) {
        startActivity(Intent(requireContext(), UploadHomeworkActivity::class.java).apply {
            putExtra("assignmentId", assignmentId)
            putExtra("assignmentTitle", title)
            putExtra("isResubmit", isResubmit)
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

@AndroidEntryPoint
class StudentSubmissionsFragment : Fragment() {

    private var _binding: FragmentSubmissionsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeworkViewModel by viewModels()

    private val adapter = SubmissionAdapter(
        items = emptyList(),
        onViewDetail = { submission ->
            if (submission.status == 4) {
                startActivity(Intent(requireContext(), GradingDetailActivity::class.java).apply {
                    putExtra("submissionId", submission.id)
                })
            } else {
                Toast.makeText(context, "批改还未完成，请稍后查看", Toast.LENGTH_SHORT).show()
            }
        },
        onResubmit = { submission ->
            startActivity(Intent(requireContext(), UploadHomeworkActivity::class.java).apply {
                putExtra("assignmentId", submission.assignmentId)
                putExtra("assignmentTitle", submission.assignmentTitle ?: "作业 #${submission.assignmentId}")
                putExtra("isResubmit", true)
            })
        }
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSubmissionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvSubmissions.adapter = adapter
        viewModel.mySubmissions.observe(viewLifecycleOwner) { page ->
            adapter.update(page.records)
        }
        viewModel.error.observe(viewLifecycleOwner) { err ->
            if (!err.isNullOrBlank()) Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
        }
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadMySubmissions()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadMySubmissions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    @Inject
    lateinit var userPreferences: UserPreferences

    private val viewModel: HomeworkViewModel by viewModels()
    private lateinit var root: LinearLayout
    private var userId: Long = 0L
    private var username: String = ""
    private var realName: String = ""
    private var submissions: List<SubmissionData> = emptyList()
    private var wrongQuestions: List<WrongQuestionData> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val scrollView = ScrollView(requireContext())
        root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
            setBackgroundColor(requireContext().getColor(R.color.background))
        }
        scrollView.addView(root)
        return scrollView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            userId = userPreferences.userId.first() ?: 0L
            username = userPreferences.username.first().orEmpty()
            realName = userPreferences.realName.first().orEmpty()
            render()
        }
        viewModel.mySubmissions.observe(viewLifecycleOwner) { page ->
            submissions = page.records
            render()
        }
        viewModel.wrongQuestions.observe(viewLifecycleOwner) {
            wrongQuestions = it
            render()
        }
        viewModel.error.observe(viewLifecycleOwner) { err ->
            if (!err.isNullOrBlank()) Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadMySubmissions()
        viewModel.loadWrongQuestions()
    }

    private fun render() {
        if (!::root.isInitialized) return
        root.removeAllViews()
        val graded = submissions.filter { it.status == 4 && it.totalScore != null }
        val avgScore = if (graded.isNotEmpty()) graded.mapNotNull { it.totalScore }.average() else null

        root.addView(sectionTitle("个人信息"))
        root.addView(infoLine("姓名", realName.ifBlank { "未填写" }))
        root.addView(infoLine("学号", if (userId > 0) userId.toString() else "-"))
        root.addView(infoLine("账号", username.ifBlank { "-" }))
        root.addView(infoLine("班级", "1 班"))

        root.addView(sectionTitle("学习概览"))
        root.addView(infoLine("已提交", "${submissions.size} 次"))
        root.addView(infoLine("已批改", "${graded.size} 次"))
        root.addView(infoLine("平均分", avgScore?.let { String.format("%.1f", it) } ?: "-"))
        root.addView(infoLine("错题数", "${wrongQuestions.size} 道"))

        root.addView(sectionTitle("历史成绩"))
        if (submissions.isEmpty()) {
            root.addView(infoLine("暂无记录", "提交作业后会显示在这里"))
        } else {
            submissions.sortedByDescending { it.submitTime.orEmpty() }.forEach {
                val title = it.assignmentTitle ?: "作业 #${it.assignmentId}"
                val score = if (it.status == 4) "${it.totalScore?.toInt() ?: "-"} 分" else statusText(it.status)
                root.addView(infoLine(title, "$score  ${it.submitTime ?: ""}"))
            }
        }

        root.addView(sectionTitle("错题查询"))
        if (wrongQuestions.isEmpty()) {
            root.addView(infoLine("暂无错题", "批改完成后，错误或部分正确的题目会显示在这里"))
        } else {
            wrongQuestions.take(20).forEach { item ->
                val title = "${item.assignmentTitle ?: "作业 #${item.assignmentId}"} 第 ${item.questionNo} 题"
                val detail = "你的答案：${item.studentAnswer.orEmpty().ifBlank { "未识别" }} / 参考答案：${item.answerKey.orEmpty()}"
                root.addView(infoLine(title, detail))
            }
        }
    }

    private fun sectionTitle(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(requireContext().getColor(R.color.text_primary))
            setPadding(0, dp(14), 0, dp(8))
        }
    }

    private fun infoLine(label: String, value: String): TextView {
        return TextView(requireContext()).apply {
            text = "$label：$value"
            textSize = 15f
            setTextColor(requireContext().getColor(R.color.text_primary))
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setBackgroundColor(requireContext().getColor(R.color.surface))
        }
    }

    private fun statusText(status: Int): String {
        return when (status) {
            0 -> "等待识别"
            1 -> "识别中"
            2 -> "识别完成"
            3 -> "批改中"
            4 -> "批改完成"
            5 -> "处理失败"
            else -> "未知"
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}

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
                1 -> "待提交"
                2 -> "已结束"
                else -> "草稿"
            }
        }
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<AssignmentData>) {
        items = newItems
        notifyDataSetChanged()
    }
}

class SubmissionAdapter(
    private var items: List<SubmissionData>,
    private val onViewDetail: (SubmissionData) -> Unit,
    private val onResubmit: (SubmissionData) -> Unit
) : RecyclerView.Adapter<SubmissionAdapter.VH>() {

    inner class VH(val binding: ItemSubmissionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemSubmissionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        with(holder.binding) {
            tvAssignmentTitle.text = item.assignmentTitle ?: "作业 #${item.assignmentId}"
            tvSubmitTime.text = item.submitTime ?: ""
            tvStatus.text = statusText(item.status)
            tvScore.text = if (item.status == 4) "${item.totalScore?.toInt() ?: "-"} 分" else "-"
            btnViewDetail.setOnClickListener { onViewDetail(item) }
            btnResubmit.setOnClickListener { onResubmit(item) }
            root.setOnClickListener { onViewDetail(item) }
        }
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<SubmissionData>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun statusText(status: Int): String {
        return when (status) {
            0 -> "等待识别"
            1 -> "识别中"
            2 -> "识别完成"
            3 -> "批改中"
            4 -> "批改完成"
            5 -> "处理失败"
            else -> "未知"
        }
    }
}
