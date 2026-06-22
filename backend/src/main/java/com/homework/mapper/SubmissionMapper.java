package com.homework.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.entity.Submission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface SubmissionMapper extends BaseMapper<Submission> {

    @Select("SELECT AVG(total_score) FROM submissions WHERE assignment_id=#{assignmentId} AND status=4 AND deleted=0")
    BigDecimal selectAvgScore(@Param("assignmentId") Long assignmentId);

    @Select("SELECT MAX(total_score) FROM submissions WHERE assignment_id=#{assignmentId} AND status=4 AND deleted=0")
    BigDecimal selectMaxScore(@Param("assignmentId") Long assignmentId);

    @Select("SELECT MIN(total_score) FROM submissions WHERE assignment_id=#{assignmentId} AND status=4 AND deleted=0")
    BigDecimal selectMinScore(@Param("assignmentId") Long assignmentId);

    @Select("SELECT COUNT(*) FROM submissions WHERE assignment_id=#{assignmentId} AND status=4 AND total_score>=60 AND deleted=0")
    Integer selectPassCount(@Param("assignmentId") Long assignmentId);

    @Select("""
            SELECT
              gr.id,
              gr.submission_id AS submissionId,
              gr.question_id AS questionId,
              gr.question_no AS questionNo,
              gr.student_answer AS studentAnswer,
              gr.is_correct AS isCorrect,
              gr.score_got AS scoreGot,
              gr.score_full AS scoreFull,
              gr.feedback,
              gr.grading_type AS gradingType,
              gr.created_time AS createdTime,
              a.id AS assignmentId,
              a.title AS assignmentTitle,
              a.subject AS subject,
              q.question_text AS questionText,
              q.answer_key AS answerKey
            FROM grading_results gr
            JOIN submissions s ON s.id = gr.submission_id
            JOIN assignments a ON a.id = s.assignment_id
            JOIN question_items q ON q.id = gr.question_id
            WHERE s.student_id = #{studentId}
              AND s.deleted = 0
              AND s.status = 4
              AND gr.is_correct IN (0, 2)
            ORDER BY gr.created_time DESC
            """)
    List<Map<String, Object>> selectWrongQuestions(@Param("studentId") Long studentId);

    @Select("""
            SELECT s.*
            FROM submissions s
            JOIN assignments a ON a.id = s.assignment_id
            JOIN classes c ON c.id = a.class_id
            WHERE s.deleted = 0
              AND a.deleted = 0
              AND c.deleted = 0
              AND (a.teacher_id = #{teacherId} OR c.teacher_id = #{teacherId})
            ORDER BY s.submit_time DESC
            """)
    Page<Submission> selectTeacherSubmissions(Page<Submission> page, @Param("teacherId") Long teacherId);
}
