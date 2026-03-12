package com.homework.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.common.R;
import com.homework.entity.GradingResult;
import com.homework.entity.Submission;
import com.homework.security.UserPrincipal;
import com.homework.service.GradingService;
import com.homework.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Tag(name = "作业提交管理")
@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;
    private final GradingService gradingService;

    @Operation(summary = "学生上传提交作业图片")
    @PostMapping("/upload")
    public R<Submission> upload(
            @RequestParam Long assignmentId,
            @RequestParam MultipartFile image,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException {
        return R.ok(submissionService.submit(assignmentId, principal.getUserId(), image));
    }

    @Operation(summary = "学生查看自己的提交列表")
    @GetMapping("/my")
    public R<Page<Submission>> mySubmissions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return R.ok(submissionService.listByStudent(principal.getUserId(), page, size));
    }

    @Operation(summary = "教师查看某作业的提交列表")
    @GetMapping("/assignment/{assignmentId}")
    public R<Page<Submission>> submissionsByAssignment(
            @PathVariable Long assignmentId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return R.ok(submissionService.listByAssignment(assignmentId, page, size));
    }

    @Operation(summary = "查看批改详情")
    @GetMapping("/{id}/grading")
    public R<Map<String, Object>> gradingDetail(@PathVariable Long id) {
        Submission submission = submissionService.getById(id);
        List<GradingResult> results = gradingService.getResults(id);
        return R.ok(Map.of("submission", submission, "gradingResults", results));
    }
}
