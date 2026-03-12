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
import java.util.*

@AndroidEntryPoint
class UploadHomeworkActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUploadHomeworkBinding
    private val viewModel: HomeworkViewModel by viewModels()
    private var assignmentId: Long = 0L
    private var currentPhotoFile: File? = null
    private var selectedImageUri: Uri? = null

    // 拍照结果处理
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            currentPhotoFile?.let {
                selectedImageUri = Uri.fromFile(it)
                binding.ivPreview.setImageURI(selectedImageUri)
                binding.btnUpload.isEnabled = true
            }
        }
    }

    // 相册选图结果处理
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                binding.ivPreview.setImageURI(uri)
                binding.btnUpload.isEnabled = true
                currentPhotoFile = uriToFile(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadHomeworkBinding.inflate(layoutInflater)
        setContentView(binding.root)

        assignmentId = intent.getLongExtra("assignmentId", 0L)
        binding.tvTitle.text = intent.getStringExtra("assignmentTitle") ?: "提交作业"

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.loading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnUpload.isEnabled = !loading && selectedImageUri != null
        }
        viewModel.uploadResult.observe(this) { result ->
            result.onSuccess { submission ->
                Toast.makeText(this, "上传成功！正在识别和批改中...", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, GradingDetailActivity::class.java)
                intent.putExtra("submissionId", submission.id)
                startActivity(intent)
                finish()
            }.onFailure {
                Toast.makeText(this, "上传失败: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnCamera.setOnClickListener { takePicture() }
        binding.btnGallery.setOnClickListener { pickFromGallery() }
        binding.btnUpload.setOnClickListener {
            currentPhotoFile?.let { viewModel.uploadSubmission(assignmentId, it) }
                ?: Toast.makeText(this, "请先选择图片", Toast.LENGTH_SHORT).show()
        }
    }

    private fun takePicture() {
        val photoFile = createImageFile()
        currentPhotoFile = photoFile
        val photoUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", photoFile)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        takePictureLauncher.launch(intent)
    }

    private fun pickFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val dir = getExternalFilesDir(null) ?: filesDir
        return File(dir, "IMG_${timestamp}.jpg")
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val tempFile = createImageFile()
            tempFile.outputStream().use { inputStream.copyTo(it) }
            tempFile
        } catch (e: Exception) {
            null
        }
    }
}
