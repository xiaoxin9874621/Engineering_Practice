package com.homework.ocr.ui.teacher

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.homework.ocr.R
import com.homework.ocr.databinding.ItemSubmissionBinding
import com.homework.ocr.model.SubmissionData
import com.homework.ocr.ui.common.GradingDetailActivity
import com.homework.ocr.viewmodel.HomeworkViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TeacherSubmissionListActivity : AppCompatActivity() {

    private val viewModel: HomeworkViewModel by viewModels()
    private var assignmentId: Long = 0L
    private lateinit var emptyView: TextView
    private lateinit var adapter: TeacherSubmissionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        assignmentId = intent.getLongExtra("assignmentId", 0L)
        val assignmentTitle = intent.getStringExtra("assignmentTitle") ?: "学生提交"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.background))
        }
        val toolbar = Toolbar(this).apply {
            title = assignmentTitle
            subtitle = "学生提交记录"
            setNavigationIcon(android.R.drawable.ic_media_previous)
            setNavigationOnClickListener { finish() }
            setBackgroundColor(getColor(R.color.primary))
            setTitleTextColor(getColor(R.color.white))
            setSubtitleTextColor(getColor(R.color.white))
        }
        root.addView(toolbar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(56)
        ))

        emptyView = TextView(this).apply {
            text = "还没有学生提交该作业"
            textSize = 16f
            setTextColor(getColor(R.color.text_secondary))
            setPadding(32, 48, 32, 24)
            visibility = View.GONE
        }
        root.addView(emptyView)

        adapter = TeacherSubmissionAdapter(emptyList()) { submission ->
            startActivity(Intent(this, GradingDetailActivity::class.java).apply {
                putExtra("submissionId", submission.id)
            })
        }
        val recyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@TeacherSubmissionListActivity)
            adapter = this@TeacherSubmissionListActivity.adapter
            setPadding(16, 16, 16, 16)
            clipToPadding = false
        }
        root.addView(recyclerView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))

        setContentView(root)
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        if (assignmentId > 0) {
            viewModel.loadAssignmentSubmissions(assignmentId)
        }
    }

    private fun setupObservers() {
        viewModel.assignmentSubmissions.observe(this) { page ->
            adapter.update(page.records)
            emptyView.visibility = if (page.records.isEmpty()) View.VISIBLE else View.GONE
        }
        viewModel.error.observe(this) { err ->
            if (!err.isNullOrBlank()) Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}

class TeacherSubmissionAdapter(
    private var items: List<SubmissionData>,
    private val onViewDetail: (SubmissionData) -> Unit
) : RecyclerView.Adapter<TeacherSubmissionAdapter.VH>() {

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
