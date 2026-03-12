package com.homework.ocr.ui.common

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.homework.ocr.databinding.ActivityGradingDetailBinding
import com.homework.ocr.model.GradingResultData
import com.homework.ocr.viewmodel.HomeworkViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GradingDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGradingDetailBinding
    private val viewModel: HomeworkViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGradingDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val submissionId = intent.getLongExtra("submissionId", 0L)
        setupObservers()
        viewModel.loadGradingDetail(submissionId)

        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupObservers() {
        viewModel.loading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
        viewModel.gradingDetail.observe(this) { detail ->
            val submission = detail.submission
            // 显示状态
            binding.tvStatus.text = when (submission.status) {
                0 -> "⏳ 等待识别"
                1 -> "🔍 识别中..."
                2 -> "✅ 识别完成"
                3 -> "📝 批改中..."
                4 -> "🎉 批改完成"
                5 -> "❌ 处理失败"
                else -> "未知"
            }
            binding.tvTotalScore.text = "总分：${submission.totalScore ?: "-"}"

            // 显示批改结果列表
            if (detail.gradingResults.isNotEmpty()) {
                binding.rvResults.layoutManager = LinearLayoutManager(this)
                binding.rvResults.adapter = GradingResultAdapter(detail.gradingResults)
            }
        }
    }
}

class GradingResultAdapter(private val results: List<GradingResultData>) :
    androidx.recyclerview.widget.RecyclerView.Adapter<GradingResultAdapter.VH>() {

    inner class VH(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val result = results[position]
        val text1 = holder.itemView.findViewById<android.widget.TextView>(android.R.id.text1)
        val text2 = holder.itemView.findViewById<android.widget.TextView>(android.R.id.text2)
        val correctStr = when (result.isCorrect) {
            1 -> "✅ 正确"
            0 -> "❌ 错误"
            2 -> "⚠️ 部分正确"
            else -> "待批改"
        }
        text1.text = "第${result.questionNo}题 $correctStr — ${result.scoreGot}/${result.scoreFull}分"
        text2.text = "学生答案：${result.studentAnswer ?: "-"}  反馈：${result.feedback ?: "-"}"
    }

    override fun getItemCount() = results.size
}
