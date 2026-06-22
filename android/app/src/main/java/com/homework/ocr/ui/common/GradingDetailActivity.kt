package com.homework.ocr.ui.common

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.homework.ocr.BuildConfig
import com.homework.ocr.R
import com.homework.ocr.databinding.ActivityGradingDetailBinding
import com.homework.ocr.databinding.ItemGradingResultBinding
import com.homework.ocr.model.AnnotationData
import com.homework.ocr.model.GradingResultData
import com.homework.ocr.model.SubmissionData
import com.homework.ocr.util.UserPreferences
import com.homework.ocr.viewmodel.HomeworkViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class GradingDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGradingDetailBinding
    private val viewModel: HomeworkViewModel by viewModels()
    private val handler = Handler(Looper.getMainLooper())
    private var submissionId: Long = 0L
    private var pollCount = 0

    @Inject
    lateinit var userPreferences: UserPreferences

    private val pollRunnable = object : Runnable {
        override fun run() {
            if (submissionId > 0L && pollCount < 15) {
                pollCount++
                viewModel.loadGradingDetail(submissionId)
                handler.postDelayed(this, 2000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGradingDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        submissionId = intent.getLongExtra("submissionId", 0L)
        binding.toolbar.setNavigationOnClickListener { finish() }
        setupAnnotationEditorVisibility()
        binding.btnSaveAnnotation.setOnClickListener {
            val content = binding.etAnnotation.text.toString().trim()
            if (content.isBlank()) {
                Toast.makeText(this, "请先输入批注内容", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.createAnnotation(submissionId, content)
            }
        }

        setupObservers()
        viewModel.loadGradingDetail(submissionId)
        viewModel.loadAnnotations(submissionId)
        handler.postDelayed(pollRunnable, 2000)
    }

    private fun setupAnnotationEditorVisibility() {
        lifecycleScope.launch {
            val isTeacher = userPreferences.role.first() == 2
            binding.etAnnotation.visibility = if (isTeacher) View.VISIBLE else View.GONE
            binding.btnSaveAnnotation.visibility = if (isTeacher) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(pollRunnable)
        super.onDestroy()
    }

    private fun setupObservers() {
        viewModel.loading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
        viewModel.gradingDetail.observe(this) { detail ->
            val submission = detail.submission
            showSubmissionImage(submission)
            binding.tvStatus.text = statusText(submission.status)
            binding.tvTotalScore.text = submission.totalScore?.let { "${it.toInt()} 分" } ?: "-"
            renderResults(detail.gradingResults)

            if (submission.status == 4 || submission.status == 5) {
                handler.removeCallbacks(pollRunnable)
            }
        }
        viewModel.annotations.observe(this) { annotations ->
            renderAnnotations(annotations)
        }
        viewModel.createAnnotationResult.observe(this) { result ->
            result.onSuccess {
                binding.etAnnotation.text?.clear()
                Toast.makeText(this, "批注已保存", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this, "保存批注失败：${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun renderResults(results: List<GradingResultData>) {
        binding.tvResultCount.text = "各题详情（${results.size} 题）"
        binding.llResults.removeAllViews()
        results.sortedBy { it.questionNo }.forEach { result ->
            val itemBinding = ItemGradingResultBinding.inflate(
                LayoutInflater.from(this),
                binding.llResults,
                false
            )
            bindResult(itemBinding, result)
            binding.llResults.addView(itemBinding.root)
        }
    }

    private fun bindResult(item: ItemGradingResultBinding, result: GradingResultData) {
        with(item) {
            tvQuestionNo.text = result.questionNo.toString()
            tvQuestionType.text = "OCR 识别与自动批改"
            tvScore.text = "${result.scoreGot}/${result.scoreFull} 分"
            tvResult.text = when (result.isCorrect) {
                1 -> "正确"
                0 -> "错误"
                2 -> "部分正确"
                else -> "待批改"
            }
            tvStudentAnswer.text = result.studentAnswer?.ifBlank { "未识别到内容" } ?: "未识别到内容"
            tvFeedback.text = result.feedback ?: ""
            tvFeedback.visibility = if (result.feedback.isNullOrBlank()) View.GONE else View.VISIBLE
        }
    }

    private fun renderAnnotations(annotations: List<AnnotationData>) {
        binding.llAnnotations.removeAllViews()
        binding.tvAnnotationHint.visibility = if (annotations.isEmpty()) View.VISIBLE else View.GONE
        annotations.forEach { annotation ->
            binding.llAnnotations.addView(TextView(this).apply {
                text = "${annotation.content}\n教师 #${annotation.teacherId}  ${annotation.createdTime.orEmpty()}"
                textSize = 14f
                setTextColor(getColor(R.color.text_primary))
                setPadding(dp(12), dp(10), dp(12), dp(10))
                setBackgroundColor(getColor(R.color.surface_variant))
            })
        }
    }

    private fun showSubmissionImage(submission: SubmissionData) {
        val imageUrl = buildImageUrl(submission.imageUrl)
        if (imageUrl.isBlank()) {
            binding.tvImageHint.text = "暂无上传图片"
            return
        }

        binding.tvImageHint.text = "上传图片已加载"
        Glide.with(this)
            .load(imageUrl)
            .fitCenter()
            .into(binding.ivSubmissionImage)
    }

    private fun buildImageUrl(imageUrl: String?): String {
        if (imageUrl.isNullOrBlank()) return ""
        if (
            imageUrl.startsWith("http://") ||
            imageUrl.startsWith("https://") ||
            imageUrl.startsWith("file://")
        ) {
            return imageUrl
        }
        if (imageUrl.startsWith("/")) {
            return BuildConfig.BASE_URL.trimEnd('/') + imageUrl
        }
        if (imageUrl.contains(":\\") || imageUrl.startsWith("/storage") || imageUrl.startsWith("/data")) {
            return Uri.fromFile(File(imageUrl)).toString()
        }
        return BuildConfig.BASE_URL + imageUrl
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
