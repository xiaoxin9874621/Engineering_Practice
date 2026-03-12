package com.homework.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.entity.Assignment;
import com.homework.entity.QuestionItem;
import com.homework.mapper.AssignmentMapper;
import com.homework.mapper.QuestionItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentMapper assignmentMapper;
    private final QuestionItemMapper questionItemMapper;

    @Transactional
    public Assignment createAssignment(Assignment assignment, List<QuestionItem> questions) {
        assignmentMapper.insert(assignment);
        if (questions != null && !questions.isEmpty()) {
            questions.forEach(q -> {
                q.setAssignmentId(assignment.getId());
                questionItemMapper.insert(q);
            });
        }
        return assignment;
    }

    public Page<Assignment> listByTeacher(Long teacherId, Integer page, Integer size) {
        return assignmentMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Assignment>()
                        .eq(Assignment::getTeacherId, teacherId)
                        .orderByDesc(Assignment::getCreatedTime)
        );
    }

    public Page<Assignment> listByClass(Long classId, Integer page, Integer size) {
        return assignmentMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Assignment>()
                        .eq(Assignment::getClassId, classId)
                        .eq(Assignment::getStatus, 1)
                        .orderByDesc(Assignment::getCreatedTime)
        );
    }

    public Assignment getById(Long id) {
        return assignmentMapper.selectById(id);
    }

    public List<QuestionItem> getQuestions(Long assignmentId) {
        return questionItemMapper.selectList(
                new LambdaQueryWrapper<QuestionItem>()
                        .eq(QuestionItem::getAssignmentId, assignmentId)
                        .orderByAsc(QuestionItem::getQuestionNo)
        );
    }

    public void updateStatus(Long id, Integer status) {
        Assignment a = new Assignment();
        a.setId(id);
        a.setStatus(status);
        assignmentMapper.updateById(a);
    }
}
