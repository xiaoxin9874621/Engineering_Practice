package com.homework.ocr.ui.student

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.homework.ocr.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OcrResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.background))
        }

        val toolbar = MaterialToolbar(this).apply {
            title = "OCR 识别结果"
            setNavigationIcon(android.R.drawable.ic_media_previous)
            setNavigationOnClickListener { finish() }
            setBackgroundColor(getColor(R.color.primary))
            setTitleTextColor(getColor(R.color.white))
        }
        root.addView(
            toolbar,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56))
        )

        root.addView(TextView(this).apply {
            text = "演示结果\n\n第1题：A\n第2题：光合作用\n第3题：需要教师复核\n\n实际识别内容会由后端 OCR 服务返回。"
            textSize = 16f
            setTextColor(getColor(R.color.text_primary))
            setPadding(32, 32, 32, 32)
        })

        setContentView(root)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
