package com.homework.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.entity.GradingResult;
import com.homework.entity.QuestionItem;
import com.homework.entity.Submission;
import com.homework.mapper.GradingResultMapper;
import com.homework.mapper.QuestionItemMapper;
import com.homework.mapper.SubmissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 批改引擎服务
 * 支持：选择题精确匹配、填空题精确匹配、简答题关键词匹配
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GradingService {

    private final SubmissionMapper submissionMapper;
    private final QuestionItemMapper questionItemMapper;
    private final GradingResultMapper gradingResultMapper;

    /**
     * 执行自动批改
     * @param submissionId 提交ID
     * @param ocrTexts     Map<题号, OCR识别文本>
     */
    @Transactional
    public BigDecimal gradeSubmission(Long submissionId, Map<Integer, String> ocrTexts) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) throw new RuntimeException("提交记录不存在");

        // 更新状态：批改中
        updateSubmissionStatus(submissionId, 3);

        List<QuestionItem> questions = questionItemMapper.selectList(
                new LambdaQueryWrapper<QuestionItem>()
                        .eq(QuestionItem::getAssignmentId, submission.getAssignmentId())
                        .orderByAsc(QuestionItem::getQuestionNo)
        );

        BigDecimal totalScore = BigDecimal.ZERO;
        List<GradingResult> results = new ArrayList<>();

        for (QuestionItem question : questions) {
            String studentAnswer = ocrTexts.getOrDefault(question.getQuestionNo(), "");
            GradingResult result = grade(question, studentAnswer);
            result.setSubmissionId(submissionId);
            totalScore = totalScore.add(result.getScoreGot());
            results.add(result);
        }

        // 保存批改结果
        results.forEach(gradingResultMapper::insert);

        // 更新提交记录
        Submission update = new Submission();
        update.setId(submissionId);
        update.setStatus(4);
        update.setTotalScore(totalScore);
        update.setGradedTime(LocalDateTime.now());
        submissionMapper.updateById(update);

        log.info("批改完成 submissionId={} totalScore={}", submissionId, totalScore);
        return totalScore;
    }

    /**
     * 批改单题
     */
    private GradingResult grade(QuestionItem question, String studentAnswer) {
        GradingResult result = new GradingResult();
        result.setQuestionId(question.getId());
        result.setQuestionNo(question.getQuestionNo());
        result.setStudentAnswer(studentAnswer);
        result.setScoreFull(question.getScore());
        result.setGradingType(1); // 自动批改

        switch (question.getGradingMode()) {
            case 1 -> gradeExactMatch(question, studentAnswer, result);
            case 2 -> gradeKeywordMatch(question, studentAnswer, result);
            default -> {
                result.setIsCorrect(null);
                result.setScoreGot(BigDecimal.ZERO);
                result.setFeedback("需要人工审核");
                result.setGradingType(2);
            }
        }
        return result;
    }

    /**
     * 精确匹配（选择题、填空题标准答案）
     */
    private void gradeExactMatch(QuestionItem question, String studentAnswer, GradingResult result) {
        String cleanStudent = normalizeAnswer(studentAnswer);
        String cleanKey = normalizeAnswer(question.getAnswerKey());
        if (cleanStudent.equals(cleanKey)) {
            result.setIsCorrect(1);
            result.setScoreGot(question.getScore());
            result.setFeedback("回答正确！");
        } else {
            result.setIsCorrect(0);
            result.setScoreGot(BigDecimal.ZERO);
            result.setFeedback("参考答案：" + question.getAnswerKey());
        }
    }

    /**
     * 关键词匹配（简答题）
     */
    private void gradeKeywordMatch(QuestionItem question, String studentAnswer, GradingResult result) {
        if (question.getKeywords() == null || question.getKeywords().isBlank()) {
            gradeExactMatch(question, studentAnswer, result);
            return;
        }
        // 解析关键词 JSON 数组，如 ["关键词1","关键词2"]
        String keywordsRaw = question.getKeywords().replace("[", "").replace("]", "").replace("\"", "");
        String[] keywords = keywordsRaw.split(",");
        int totalKeywords = keywords.length;
        int matchedCount = 0;
        for (String keyword : keywords) {
            if (studentAnswer.contains(keyword.trim())) {
                matchedCount++;
            }
        }
        double ratio = (double) matchedCount / totalKeywords;
        BigDecimal scoreGot = question.getScore()
                .multiply(BigDecimal.valueOf(ratio))
                .setScale(2, RoundingMode.HALF_UP);
        result.setScoreGot(scoreGot);
        if (ratio >= 1.0) {
            result.setIsCorrect(1);
            result.setFeedback("回答完整，包含所有关键点。");
        } else if (ratio > 0) {
            result.setIsCorrect(2);
            result.setFeedback(String.format("包含 %d/%d 个关键点，得 %.1f 分。",
                    matchedCount, totalKeywords, scoreGot.doubleValue()));
        } else {
            result.setIsCorrect(0);
            result.setFeedback("未包含关键词，参考答案：" + question.getAnswerKey());
        }
    }

    private String normalizeAnswer(String answer) {
        if (answer == null) return "";
        return answer.trim()
                .toUpperCase()
                .replace("，", ",")
                .replace("。", ".")
                .replaceAll("\\s+", "");
    }

    private void updateSubmissionStatus(Long submissionId, int status) {
        Submission update = new Submission();
        update.setId(submissionId);
        update.setStatus(status);
        submissionMapper.updateById(update);
    }

    public List<GradingResult> getResults(Long submissionId) {
        return gradingResultMapper.selectList(
                new LambdaQueryWrapper<GradingResult>()
                        .eq(GradingResult::getSubmissionId, submissionId)
                        .orderByAsc(GradingResult::getQuestionNo)
        );
    }
}
