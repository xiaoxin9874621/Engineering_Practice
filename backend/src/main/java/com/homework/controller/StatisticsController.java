package com.homework.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.common.R;
import com.homework.entity.Submission;
import com.homework.mapper.SubmissionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "统计分析")
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final SubmissionMapper submissionMapper;

    @Operation(summary = "某作业统计（avg/max/min/passRate）")
    @GetMapping("/assignment/{assignmentId}")
    public R<Map<String, Object>> assignmentStats(@PathVariable Long assignmentId) {
        long total = submissionMapper.selectCount(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getAssignmentId, assignmentId));
        long graded = submissionMapper.selectCount(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getAssignmentId, assignmentId)
                .eq(Submission::getStatus, 4));
        BigDecimal avg = submissionMapper.selectAvgScore(assignmentId);
        BigDecimal max = submissionMapper.selectMaxScore(assignmentId);
        BigDecimal min = submissionMapper.selectMinScore(assignmentId);
        Integer passCount = submissionMapper.selectPassCount(assignmentId);

        Map<String, Object> data = new HashMap<>();
        data.put("totalCount", total);
        data.put("gradedCount", graded);
        data.put("avgScore", avg);
        data.put("maxScore", max);
        data.put("minScore", min);
        data.put("passRate", graded > 0 ? (passCount * 100.0 / graded) : 0);
        return R.ok(data);
    }

    @Operation(summary = "学生历次作业成绩趋势")
    @GetMapping("/student/{studentId}/trend")
    public R<?> studentTrend(@PathVariable Long studentId) {
        var list = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getStudentId, studentId)
                .eq(Submission::getStatus, 4)
                .orderByAsc(Submission::getSubmitTime)
                .select(Submission::getAssignmentId, Submission::getTotalScore, Submission::getSubmitTime));
        return R.ok(list);
    }
}
