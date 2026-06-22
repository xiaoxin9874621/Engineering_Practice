package com.homework.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.entity.Annotation;
import com.homework.mapper.AnnotationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnotationService {

    private final AnnotationMapper annotationMapper;

    public List<Annotation> listBySubmission(Long submissionId) {
        return annotationMapper.selectList(new LambdaQueryWrapper<Annotation>()
                .eq(Annotation::getSubmissionId, submissionId)
                .orderByDesc(Annotation::getCreatedTime));
    }

    public Annotation create(Long submissionId, Long teacherId, String content,
                             Integer positionX, Integer positionY, String color) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("批注内容不能为空");
        }
        Annotation annotation = new Annotation();
        annotation.setSubmissionId(submissionId);
        annotation.setTeacherId(teacherId);
        annotation.setContent(content.trim());
        annotation.setPositionX(positionX);
        annotation.setPositionY(positionY);
        annotation.setColor(color == null || color.isBlank() ? "#FF0000" : color);
        annotationMapper.insert(annotation);
        return annotation;
    }
}
