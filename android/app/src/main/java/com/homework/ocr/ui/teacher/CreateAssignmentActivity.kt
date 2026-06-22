package com.homework.ocr.ui.teacher

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.homework.ocr.R
import com.homework.ocr.model.CreateQuestionRequest
import com.homework.ocr.viewmodel.HomeworkViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateAssignmentActivity : AppCompatActivity() {

    private val viewModel: HomeworkViewModel by viewModels()
    private lateinit var titleInput: TextInputEditText
    private lateinit var subjectInput: TextInputEditText
    private lateinit var classIdInput: TextInputEditText
    private lateinit var questionsContainer: LinearLayout
    private val questionForms = mutableListOf<QuestionForm>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.background))
        }

        val toolbar = MaterialToolbar(this).apply {
            title = "创建作业"
            setNavigationIcon(android.R.drawable.ic_media_previous)
            setNavigationOnClickListener { finish() }
            setBackgroundColor(getColor(R.color.primary))
            setTitleTextColor(getColor(R.color.white))
        }
        root.addView(
            toolbar,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56))
        )

        val scrollView = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
        }
        scrollView.addView(content)
        root.addView(
            scrollView,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        content.addView(label("创建后会提交到真实后端。默认演示班级 ID 为 1。"))
        titleInput = editText("例如：数学周测：一次函数")
        subjectInput = editText("例如：数学")
        classIdInput = editText("例如：1").apply { setText("1") }
        content.addView(input("作业标题", titleInput))
        content.addView(input("学科", subjectInput))
        content.addView(input("班级 ID", classIdInput))

        questionsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        content.addView(questionsContainer)

        content.addView(MaterialButton(this).apply {
            text = "添加题目"
            setOnClickListener { addQuestionForm() }
        })

        content.addView(MaterialButton(this).apply {
            text = "提交到后端"
            setOnClickListener { submit() }
        })

        addQuestionForm("选择正确答案", "A", "10", "1")
        addQuestionForm("填写关键概念", "光合作用", "20", "2")
        addQuestionForm("简述一次函数图像特点", "两点,直线,函数", "70", "3")

        observeResult()
        setContentView(root)
    }

    private fun addQuestionForm(
        questionText: String = "",
        answerText: String = "",
        scoreText: String = "10",
        typeText: String = "3"
    ) {
        val number = questionForms.size + 1
        val form = QuestionForm(
            typeInput = editText("1=选择题，2=填空题，3=简答题").apply { setText(typeText) },
            scoreInput = editText("例如：10").apply { setText(scoreText) },
            questionInput = editText("输入题目内容", true).apply { setText(questionText) },
            answerInput = editText("选择/填空填标准答案；简答填关键词，用逗号分隔", true).apply { setText(answerText) }
        )
        questionForms.add(form)

        val card = MaterialCardView(this).apply {
            radius = dp(8).toFloat()
            cardElevation = dp(1).toFloat()
            setContentPadding(dp(14), dp(14), dp(14), dp(14))
            useCompatPadding = true
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        box.addView(TextView(this).apply {
            text = "第 $number 题"
            textSize = 16f
            setTextColor(getColor(R.color.text_primary))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        box.addView(input("题型", form.typeInput))
        box.addView(input("分值", form.scoreInput))
        box.addView(input("题目", form.questionInput))
        box.addView(input("参考答案 / 关键词", form.answerInput))
        card.addView(box)
        questionsContainer.addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun submit() {
        val title = titleInput.text.toString().trim()
        val subject = subjectInput.text.toString().trim().ifBlank { "综合" }
        val classId = classIdInput.text.toString().trim().toLongOrNull() ?: 1L
        if (title.isBlank()) {
            Toast.makeText(this, "请填写作业标题", Toast.LENGTH_SHORT).show()
            return
        }

        val questions = questionForms.mapIndexedNotNull { index, form ->
            val type = form.typeInput.text.toString().trim().toIntOrNull()?.coerceIn(1, 3) ?: 3
            val score = form.scoreInput.text.toString().trim().toDoubleOrNull() ?: 10.0
            val questionText = form.questionInput.text.toString().trim()
            val answerText = form.answerInput.text.toString().trim()
            if (questionText.isBlank() || answerText.isBlank()) return@mapIndexedNotNull null
            CreateQuestionRequest(
                questionNo = index + 1,
                questionType = type,
                questionText = questionText,
                answerKey = answerText,
                score = score,
                gradingMode = if (type == 3) 2 else 1,
                keywords = if (type == 3) toKeywordJson(answerText) else null
            )
        }

        if (questions.isEmpty()) {
            Toast.makeText(this, "请至少填写一道完整题目", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.createAssignment(title, subject, classId, questions)
    }

    private fun toKeywordJson(text: String): String {
        return text.split(",", "，", "、", " ")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
    }

    private fun observeResult() {
        viewModel.createAssignmentResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "作业已创建", Toast.LENGTH_SHORT).show()
                finish()
            }.onFailure {
                Toast.makeText(this, "创建失败：${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun label(textValue: String): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = 14f
            setTextColor(getColor(R.color.text_secondary))
            setPadding(0, 0, 0, dp(12))
        }
    }

    private fun input(hintValue: String, editText: TextInputEditText): TextInputLayout {
        return TextInputLayout(this).apply {
            hint = hintValue
            setPadding(0, 0, 0, dp(12))
            addView(editText)
        }
    }

    private fun editText(placeholder: String, multiLine: Boolean = false): TextInputEditText {
        return TextInputEditText(this).apply {
            hint = placeholder
            minLines = if (multiLine) 2 else 1
            maxLines = if (multiLine) 5 else 1
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private data class QuestionForm(
        val typeInput: TextInputEditText,
        val scoreInput: TextInputEditText,
        val questionInput: TextInputEditText,
        val answerInput: TextInputEditText
    )
}
