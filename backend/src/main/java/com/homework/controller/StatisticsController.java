package com.homework.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.common.R;
import com.homework.entity.Assignment;
import com.homework.entity.Classes;
import com.homework.entity.Submission;
import com.homework.mapper.AssignmentMapper;
import com.homework.mapper.ClassesMapper;
import com.homework.mapper.SubmissionMapper;
import com.homework.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "统计分析")
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final SubmissionMapper submissionMapper;
    private final ClassesMapper classesMapper;
    private final AssignmentMapper assignmentMapper;

    @Operation(summary = "某作业统计")
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

    @Operation(summary = "教师按班级查看统计")
    @GetMapping("/teacher/classes")
    public R<List<Map<String, Object>>> teacherClassStats(@AuthenticationPrincipal UserPrincipal principal) {
        List<Classes> classes = classesMapper.selectList(new LambdaQueryWrapper<Classes>()
                .eq(Classes::getTeacherId, principal.getUserId())
                .eq(Classes::getDeleted, 0)
                .orderByAsc(Classes::getId));

        List<Map<String, Object>> result = classes.stream().map(clazz -> {
            List<Long> assignmentIds = assignmentMapper.selectList(new LambdaQueryWrapper<Assignment>()
                            .eq(Assignment::getClassId, clazz.getId())
                            .eq(Assignment::getTeacherId, principal.getUserId())
                            .eq(Assignment::getDeleted, 0))
                    .stream()
                    .map(Assignment::getId)
                    .toList();

            long submissionCount = 0;
            long gradedCount = 0;
            long passCount = 0;
            double avgScore = 0.0;
            if (!assignmentIds.isEmpty()) {
                List<Submission> submissions = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                        .in(Submission::getAssignmentId, assignmentIds)
                        .eq(Submission::getDeleted, 0));
                submissionCount = submissions.size();
                List<Submission> graded = submissions.stream()
                        .filter(s -> s.getStatus() != null && s.getStatus() == 4 && s.getTotalScore() != null)
                        .toList();
                gradedCount = graded.size();
                passCount = graded.stream()
                        .filter(s -> s.getTotalScore().doubleValue() >= 60.0)
                        .count();
                avgScore = graded.isEmpty() ? 0.0 : graded.stream()
                        .mapToDouble(s -> s.getTotalScore().doubleValue())
                        .average()
                        .orElse(0.0);
            }

            Map<String, Object> item = new HashMap<>();
            item.put("classId", clazz.getId());
            item.put("className", clazz.getClassName());
            item.put("assignmentCount", assignmentIds.size());
            item.put("submissionCount", submissionCount);
            item.put("gradedCount", gradedCount);
            item.put("avgScore", avgScore);
            item.put("passRate", gradedCount > 0 ? passCount * 100.0 / gradedCount : 0.0);
            return item;
        }).toList();

        return R.ok(result);
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
