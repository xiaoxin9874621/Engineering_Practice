package com.homework.ocr.ui.teacher

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.homework.ocr.R
import com.homework.ocr.databinding.FragmentTeacherAssignmentsBinding
import com.homework.ocr.model.AssignmentData
import com.homework.ocr.viewmodel.HomeworkViewModel
import com.homework.ocr.ui.student.UploadHomeworkActivity
import dagger.hilt.android.AndroidEntryPoint

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
            // 点击进入提交列表
            Toast.makeText(context, "作业：${assignment.title}", Toast.LENGTH_SHORT).show()
        }
        binding.rvAssignments.adapter = adapter

        viewModel.teacherAssignments.observe(viewLifecycleOwner) { page ->
            adapter.update(page.records)
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            if (err != null) Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadTeacherAssignments()
            binding.swipeRefresh.isRefreshing = false
        }

        viewModel.loadTeacherAssignments()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// TeacherSubmissionsFragment
@AndroidEntryPoint
class TeacherSubmissionsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return TextView(context).apply { text = "提交记录（请先选择作业）"; textSize = 16f; setPadding(48, 48, 48, 48) }
    }
}

// StatisticsFragment
@AndroidEntryPoint
class StatisticsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return TextView(context).apply { text = "统计分析（请先选择作业）"; textSize = 16f; setPadding(48, 48, 48, 48) }
    }
}

// Assignment RecyclerView Adapter
class AssignmentAdapter(
    private var items: List<AssignmentData>,
    private val onClick: (AssignmentData) -> Unit
) : RecyclerView.Adapter<AssignmentAdapter.VH>() {

    inner class VH(val binding: com.homework.ocr.databinding.ItemAssignmentBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = com.homework.ocr.databinding.ItemAssignmentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        with(holder.binding) {
            tvAssignmentTitle.text = item.title
            tvSubject.text = item.subject ?: "-"
            tvTotalScore.text = "满分 ${item.totalScore.toInt()} 分"
            tvDeadline.text = item.deadline?.let { "截止：$it" } ?: "无截止时间"
            tvAssignmentStatus.text = when (item.status) {
                0 -> "草稿"; 1 -> "已发布"; 2 -> "已结束"; else -> "未知"
            }
            tvAssignmentStatus.setBackgroundResource(
                when (item.status) {
                    1 -> com.homework.ocr.R.drawable.bg_status_label
                    else -> com.homework.ocr.R.drawable.bg_status_label
                }
            )
        }
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<AssignmentData>) {
        items = newItems
        notifyDataSetChanged()
    }
}
