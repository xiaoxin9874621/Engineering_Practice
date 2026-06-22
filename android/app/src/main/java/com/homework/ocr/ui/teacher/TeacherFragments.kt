package com.homework.ocr.ui.teacher

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.homework.ocr.R
import com.homework.ocr.databinding.FragmentSubmissionsBinding
import com.homework.ocr.databinding.FragmentTeacherAssignmentsBinding
import com.homework.ocr.databinding.ItemAssignmentBinding
import com.homework.ocr.databinding.ItemSubmissionBinding
import com.homework.ocr.model.AssignmentData
import com.homework.ocr.model.ClassStatsData
import com.homework.ocr.model.SubmissionData
import com.homework.ocr.ui.common.GradingDetailActivity
import com.homework.ocr.viewmodel.HomeworkViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class TeacherAssignmentsFragment : Fragment() {

    private var _binding: FragmentTeacherAssignmentsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeworkViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTeacherAssignmentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = AssignmentAdapter(emptyList()) { assignment ->
            startActivity(Intent(requireContext(), TeacherSubmissionListActivity::class.java).apply {
                putExtra("assignmentId", assignment.id)
                putExtra("assignmentTitle", assignment.title)
            })
        }
        binding.rvAssignments.adapter = adapter
        binding.btnCreateAssignment.setOnClickListener {
            startActivity(Intent(requireContext(), CreateAssignmentActivity::class.java))
        }

        viewModel.teacherAssignments.observe(viewLifecycleOwner) { page ->
            adapter.update(page.records)
            val published = page.records.count { it.status == 1 }
            binding.tvStatTitle.text = "作业概览：共 ${page.total} 个，已发布 $published 个"
            binding.tvAvgScore.text = page.records.size.toString()
            binding.tvPassRate.text = "${published}/${page.records.size}"
            binding.tvGradedCount.text = published.toString()
        }
        viewModel.error.observe(viewLifecycleOwner) { err ->
            if (!err.isNullOrBlank()) Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
        }
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadTeacherAssignments()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadTeacherAssignments()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

@AndroidEntryPoint
class TeacherSubmissionsFragment : Fragment() {

    private var _binding: FragmentSubmissionsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeworkViewModel by viewModels()
    private val adapter = TeacherSubmissionFeedAdapter(emptyList()) { submission ->
        startActivity(Intent(requireContext(), GradingDetailActivity::class.java).apply {
            putExtra("submissionId", submission.id)
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSubmissionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvSubmissions.adapter = adapter
        viewModel.assignmentSubmissions.observe(viewLifecycleOwner) { page ->
            adapter.update(page.records)
        }
        viewModel.error.observe(viewLifecycleOwner) { err ->
            if (!err.isNullOrBlank()) Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
        }
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadTeacherSubmissions()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadTeacherSubmissions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

@AndroidEntryPoint
class StatisticsFragment : Fragment() {

    private val viewModel: HomeworkViewModel by viewModels()
    private lateinit var root: LinearLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val scroll = ScrollView(requireContext())
        root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16))
            setBackgroundColor(requireContext().getColor(R.color.background))
        }
        scroll.addView(root)
        return scroll
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.classStats.observe(viewLifecycleOwner) { render(it) }
        viewModel.error.observe(viewLifecycleOwner) { err ->
            if (!err.isNullOrBlank()) Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadTeacherClassStats()
    }

    private fun render(stats: List<ClassStatsData>) {
        root.removeAllViews()
        root.addView(sectionTitle("统计分析"))

        if (stats.isEmpty()) {
            root.addView(infoCard("暂无班级数据", "教师账号还没有绑定班级，或班级下还没有作业与提交。"))
            return
        }

        val avgScore = stats.map { it.avgScore }.average()
        val avgPassRate = stats.map { it.passRate }.average()
        val totalSubmissions = stats.fold(0) { total, item -> total + item.submissionCount }
        val totalGraded = stats.fold(0) { total, item -> total + item.gradedCount }

        root.addView(metricRow(
            metricCard("平均分", String.format(Locale.CHINA, "%.1f", avgScore), "班级整体"),
            metricCard("及格率", "${avgPassRate.toInt()}%", "已批改作业"),
            metricCard("批改进度", "$totalGraded/$totalSubmissions", "总提交")
        ))

        root.addView(sectionTitle("班级对比"))
        stats.forEach { stat ->
            root.addView(classStatCard(stat))
        }
    }

    private fun classStatCard(stat: ClassStatsData): View {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14))
            setBackgroundColor(requireContext().getColor(R.color.surface))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
        }
        card.addView(TextView(requireContext()).apply {
            text = stat.className
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(requireContext().getColor(R.color.text_primary))
        })
        card.addView(TextView(requireContext()).apply {
            text = "作业 ${stat.assignmentCount} 个  提交 ${stat.submissionCount} 次  已批改 ${stat.gradedCount} 次"
            textSize = 13f
            setTextColor(requireContext().getColor(R.color.text_secondary))
            setPadding(0, dp(6), 0, dp(10))
        })
        card.addView(progressBlock("平均分", stat.avgScore, 100.0, R.color.primary))
        card.addView(progressBlock("及格率", stat.passRate, 100.0, R.color.correct))
        return card
    }

    private fun progressBlock(label: String, value: Double, max: Double, colorRes: Int): View {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
        }
        container.addView(TextView(requireContext()).apply {
            text = "$label  ${String.format(Locale.CHINA, "%.1f", value)}${if (label == "及格率") "%" else ""}"
            textSize = 13f
            setTextColor(requireContext().getColor(R.color.text_primary))
        })
        container.addView(LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(requireContext().getColor(R.color.divider))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(8)
            ).apply { topMargin = dp(6) }
            addView(View(requireContext()).apply {
                setBackgroundColor(requireContext().getColor(colorRes))
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    ((value / max).coerceIn(0.0, 1.0)).toFloat()
                )
            })
            addView(View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (1f - ((value / max).coerceIn(0.0, 1.0)).toFloat())
                )
            })
        })
        return container
    }

    private fun metricRow(vararg views: View): View {
        val scroll = HorizontalScrollView(requireContext()).apply {
            isHorizontalScrollBarEnabled = false
        }
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        views.forEachIndexed { index, view ->
            val params = LinearLayout.LayoutParams(dp(140), LinearLayout.LayoutParams.WRAP_CONTENT)
            if (index < views.lastIndex) params.marginEnd = dp(10)
            row.addView(view, params)
        }
        scroll.addView(row)
        return scroll
    }

    private fun metricCard(title: String, value: String, subtitle: String): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14))
            setBackgroundColor(requireContext().getColor(R.color.surface))
            addView(TextView(requireContext()).apply {
                text = title
                textSize = 13f
                setTextColor(requireContext().getColor(R.color.text_secondary))
            })
            addView(TextView(requireContext()).apply {
                text = value
                textSize = 28f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(requireContext().getColor(R.color.text_primary))
                setPadding(0, dp(6), 0, dp(2))
            })
            addView(TextView(requireContext()).apply {
                text = subtitle
                textSize = 12f
                setTextColor(requireContext().getColor(R.color.text_hint))
            })
        }
    }

    private fun infoCard(title: String, message: String): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14))
            setBackgroundColor(requireContext().getColor(R.color.surface))
            addView(TextView(requireContext()).apply {
                text = title
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(requireContext().getColor(R.color.text_primary))
            })
            addView(TextView(requireContext()).apply {
                text = message
                textSize = 14f
                setTextColor(requireContext().getColor(R.color.text_secondary))
                setPadding(0, dp(8), 0, 0)
            })
        }
    }

    private fun sectionTitle(text: String): TextView = TextView(requireContext()).apply {
        this.text = text
        textSize = 18f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(requireContext().getColor(R.color.text_primary))
        setPadding(0, 0, 0, dp(12))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

class AssignmentAdapter(
    private var items: List<AssignmentData>,
    private val onClick: (AssignmentData) -> Unit
) : RecyclerView.Adapter<AssignmentAdapter.VH>() {

    inner class VH(val binding: ItemAssignmentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemAssignmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        with(holder.binding) {
            tvAssignmentTitle.text = item.title
            tvSubject.text = item.subject ?: "-"
            tvTotalScore.text = "满分 ${item.totalScore.toInt()} 分"
            tvDeadline.text = item.deadline?.let { "截止：$it" } ?: "无截止时间"
            tvAssignmentStatus.text = when (item.status) {
                0 -> "草稿"
                1 -> "已发布"
                2 -> "已结束"
                else -> "未知"
            }
            tvAssignmentStatus.setBackgroundResource(R.drawable.bg_status_label)
        }
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<AssignmentData>) {
        items = newItems
        notifyDataSetChanged()
    }
}

class TeacherSubmissionFeedAdapter(
    private var items: List<SubmissionData>,
    private val onViewDetail: (SubmissionData) -> Unit
) : RecyclerView.Adapter<TeacherSubmissionFeedAdapter.VH>() {

    inner class VH(val binding: ItemSubmissionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemSubmissionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        with(holder.binding) {
            tvAssignmentTitle.text = "学生 #${item.studentId} - ${item.assignmentTitle ?: "作业 #${item.assignmentId}"}"
            tvSubmitTime.text = item.submitTime ?: ""
            tvStatus.text = statusText(item.status)
            tvScore.text = if (item.status == 4) "${item.totalScore?.toInt() ?: "-"} 分" else "-"
            btnViewDetail.text = "查看批改"
            btnViewDetail.setOnClickListener { onViewDetail(item) }
            btnResubmit.visibility = View.GONE
            root.setOnClickListener { onViewDetail(item) }
        }
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<SubmissionData>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun statusText(status: Int): String = when (status) {
        0 -> "等待识别"
        1 -> "识别中"
        2 -> "识别完成"
        3 -> "批改中"
        4 -> "批改完成"
        5 -> "处理失败"
        else -> "未知"
    }
}
