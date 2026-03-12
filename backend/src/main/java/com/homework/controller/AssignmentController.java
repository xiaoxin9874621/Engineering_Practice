package com.homework.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.common.R;
import com.homework.entity.Assignment;
import com.homework.entity.QuestionItem;
import com.homework.security.UserPrincipal;
import com.homework.service.AssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "作业管理")
@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @Operation(summary = "教师创建作业")
    @PostMapping
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public R<Assignment> create(@RequestBody Map<String, Object> body,
                                @AuthenticationPrincipal UserPrincipal principal) {
        Assignment assignment = new Assignment();
        assignment.setTitle((String) body.get("title"));
        assignment.setDescription((String) body.get("description"));
        assignment.setClassId(Long.parseLong(body.get("classId").toString()));
        assignment.setTeacherId(principal.getUserId());
        assignment.setSubject((String) body.get("subject"));
        assignment.setStatus(1);

        @SuppressWarnings("unchecked")
        List<QuestionItem> questions = (List<QuestionItem>) body.get("questions");
        return R.ok(assignmentService.createAssignment(assignment, questions));
    }

    @Operation(summary = "教师查询自己的作业列表")
    @GetMapping("/teacher")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public R<Page<Assignment>> listByTeacher(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return R.ok(assignmentService.listByTeacher(principal.getUserId(), page, size));
    }

    @Operation(summary = "学生根据班级查询作业列表")
    @GetMapping("/class/{classId}")
    public R<Page<Assignment>> listByClass(
            @PathVariable Long classId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return R.ok(assignmentService.listByClass(classId, page, size));
    }

    @Operation(summary = "查询作业详情（含题目）")
    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        Assignment assignment = assignmentService.getById(id);
        List<QuestionItem> questions = assignmentService.getQuestions(id);
        return R.ok(Map.of("assignment", assignment, "questions", questions));
    }

    @Operation(summary = "更新作业状态（发布/结束）")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        assignmentService.updateStatus(id, status);
        return R.ok();
    }
}
