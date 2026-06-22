package com.homework.controller;

import com.homework.common.R;
import com.homework.entity.Annotation;
import com.homework.security.UserPrincipal;
import com.homework.service.AnnotationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "教师批注")
@RestController
@RequestMapping("/api/annotations")
@RequiredArgsConstructor
public class AnnotationController {

    private final AnnotationService annotationService;

    @Operation(summary = "查看某次提交的批注")
    @GetMapping("/submission/{submissionId}")
    public R<List<Annotation>> listBySubmission(@PathVariable Long submissionId) {
        return R.ok(annotationService.listBySubmission(submissionId));
    }

    @Operation(summary = "教师新增批注")
    @PostMapping
    public R<Annotation> create(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal.getRole() == null || principal.getRole() != 2) {
            throw new IllegalArgumentException("只有教师可以新增批注");
        }
        Long submissionId = toLong(body.get("submissionId"));
        String content = (String) body.get("content");
        Integer positionX = toInteger(body.get("positionX"));
        Integer positionY = toInteger(body.get("positionY"));
        String color = (String) body.getOrDefault("color", "#FF0000");
        return R.ok(annotationService.create(
                submissionId,
                principal.getUserId(),
                content,
                positionX,
                positionY,
                color
        ));
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        return Long.parseLong(value.toString());
    }

    private Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        return Integer.parseInt(value.toString());
    }
}
