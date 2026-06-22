package com.homework.ocr.ui.student

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.homework.ocr.databinding.ActivityUploadHomeworkBinding
import com.homework.ocr.ui.common.GradingDetailActivity
import com.homework.ocr.viewmodel.HomeworkViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class UploadHomeworkActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUploadHomeworkBinding
    private val viewModel: HomeworkViewModel by viewModels()
    private var assignmentId: Long = 0L
    private var isResubmit: Boolean = false
    private var currentPhotoFile: File? = null
    private var selectedImageUri: Uri? = null

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            currentPhotoFile?.let { file ->
                selectedImageUri = Uri.fromFile(file)
                showSelectedImage(selectedImageUri!!)
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@registerForActivityResult

        val copiedFile = uriToFile(uri)
        if (copiedFile == null) {
            Toast.makeText(this, "图片读取失败，请重新选择", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }

        selectedImageUri = uri
        currentPhotoFile = copiedFile
        showSelectedImage(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadHomeworkBinding.inflate(layoutInflater)
        setContentView(binding.root)

        assignmentId = intent.getLongExtra("assignmentId", 0L)
        isResubmit = intent.getBooleanExtra("isResubmit", false)
        val title = intent.getStringExtra("assignmentTitle") ?: "提交作业"
        binding.tvTitle.text = if (isResubmit) "重新提交：$title" else title
        binding.btnUpload.text = if (isResubmit) "重新提交作业" else "提交作业"

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.loading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnUpload.isEnabled = !loading && currentPhotoFile != null
        }

        viewModel.uploadResult.observe(this) { result ->
            result.onSuccess { submission ->
                Toast.makeText(this, "提交成功，正在识别和批改...", Toast.LENGTH_SHORT).show()
                startActivity(
                    Intent(this, GradingDetailActivity::class.java)
                        .putExtra("submissionId", submission.id)
                )
                finish()
            }.onFailure {
                Toast.makeText(this, "提交失败：${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnCamera.setOnClickListener { takePicture() }
        binding.btnGallery.setOnClickListener { pickFromGallery() }
        binding.btnUpload.setOnClickListener {
            currentPhotoFile?.let { viewModel.uploadSubmission(assignmentId, it) }
                ?: Toast.makeText(this, "请先选择图片", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSelectedImage(uri: Uri) {
        binding.ivPreview.setImageURI(uri)
        binding.llPlaceholder.visibility = View.GONE
        binding.btnUpload.isEnabled = true
    }

    private fun takePicture() {
        val photoFile = createImageFile("jpg")
        currentPhotoFile = photoFile
        val photoUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", photoFile)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            .putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        takePictureLauncher.launch(intent)
    }

    private fun pickFromGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun createImageFile(extension: String = "jpg"): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val dir = File(cacheDir, "submissions").apply { mkdirs() }
        return File(dir, "IMG_${timestamp}.${extension.lowercase(Locale.ROOT)}")
    }

    private fun uriToFile(uri: Uri): File? {
        val extension = when (contentResolver.getType(uri)) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        return try {
            val tempFile = createImageFile(extension)
            contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            tempFile
        } catch (e: Exception) {
            null
        }
    }
}
