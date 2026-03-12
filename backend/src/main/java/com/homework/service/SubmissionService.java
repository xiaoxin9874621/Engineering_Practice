package com.homework.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.entity.Submission;
import com.homework.mapper.SubmissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionMapper submissionMapper;
    private final OcrService ocrService;
    private final GradingService gradingService;

    @Value("${file.upload.path}")
    private String uploadPath;

    /**
     * 提交作业（上传图片）
     */
    public Submission submit(Long assignmentId, Long studentId, MultipartFile imageFile) throws IOException {
        // 检查是否已提交
        Submission existing = submissionMapper.selectOne(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getAssignmentId, assignmentId)
                .eq(Submission::getStudentId, studentId));
        if (existing != null) {
            throw new RuntimeException("您已提交过此作业，请勿重复提交");
        }

        // 保存文件
        String imagePath = saveImage(imageFile, assignmentId, studentId);
        String imageUrl = "/uploads/" + assignmentId + "/" + new File(imagePath).getName();

        Submission submission = new Submission();
        submission.setAssignmentId(assignmentId);
        submission.setStudentId(studentId);
        submission.setImagePath(imagePath);
        submission.setImageUrl(imageUrl);
        submission.setStatus(0);
        submission.setSubmitTime(LocalDateTime.now());
        submissionMapper.insert(submission);

        // 异步处理 OCR 识别和批改
        processAsync(submission.getId(), imageFile);

        return submission;
    }

    @Async
    public void processAsync(Long submissionId, MultipartFile imageFile) {
        try {
            // 更新状态：识别中
            updateStatus(submissionId, 1);
            Map<Integer, String> ocrTexts = ocrService.recognize(imageFile);
            updateStatus(submissionId, 2);

            // 自动批改
            gradingService.gradeSubmission(submissionId, ocrTexts);
        } catch (Exception e) {
            log.error("作业处理失败 submissionId={}", submissionId, e);
            updateStatus(submissionId, 5);
        }
    }

    private void updateStatus(Long submissionId, int status) {
        Submission s = new Submission();
        s.setId(submissionId);
        s.setStatus(status);
        submissionMapper.updateById(s);
    }

    public Page<Submission> listByStudent(Long studentId, Integer page, Integer size) {
        return submissionMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getStudentId, studentId)
                        .orderByDesc(Submission::getSubmitTime)
        );
    }

    public Page<Submission> listByAssignment(Long assignmentId, Integer page, Integer size) {
        return submissionMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getAssignmentId, assignmentId)
                        .orderByDesc(Submission::getSubmitTime)
        );
    }

    public Submission getById(Long id) {
        return submissionMapper.selectById(id);
    }

    private String saveImage(MultipartFile file, Long assignmentId, Long studentId) throws IOException {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String dir = uploadPath + File.separator + assignmentId;
        File dirFile = new File(dir);
        if (!dirFile.exists()) dirFile.mkdirs();
        String suffix = file.getOriginalFilename() != null && file.getOriginalFilename().contains(".")
                ? file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."))
                : ".jpg";
        String filename = studentId + "_" + dateStr + "_" + UUID.randomUUID().toString().substring(0, 8) + suffix;
        String fullPath = dir + File.separator + filename;
        file.transferTo(new File(fullPath));
        return fullPath;
    }
}
