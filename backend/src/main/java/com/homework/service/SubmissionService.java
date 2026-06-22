package com.homework.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.entity.QuestionItem;
import com.homework.entity.Submission;
import com.homework.mapper.AssignmentMapper;
import com.homework.mapper.GradingResultMapper;
import com.homework.mapper.QuestionItemMapper;
import com.homework.mapper.SubmissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionMapper submissionMapper;
    private final AssignmentMapper assignmentMapper;
    private final QuestionItemMapper questionItemMapper;
    private final GradingResultMapper gradingResultMapper;
    private final OcrService ocrService;
    private final GradingService gradingService;

    @Value("${file.upload.path}")
    private String uploadPath;

    @Value("${ocr.mock-when-unavailable:true}")
    private boolean mockWhenOcrUnavailable;

    public Submission submit(Long assignmentId, Long studentId, MultipartFile imageFile) throws IOException {
        Submission existing = submissionMapper.selectOne(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getAssignmentId, assignmentId)
                .eq(Submission::getStudentId, studentId));
        String imagePath = saveImage(imageFile, assignmentId, studentId);
        String imageUrl = "/uploads/" + assignmentId + "/" + new File(imagePath).getName();

        LocalDateTime now = LocalDateTime.now();
        Submission submission = existing != null ? existing : new Submission();
        if (existing == null) {
            submission.setAssignmentId(assignmentId);
            submission.setStudentId(studentId);
            submission.setCreatedTime(now);
        } else {
            clearGradingResults(existing.getId());
        }
        submission.setImagePath(imagePath);
        submission.setImageUrl(imageUrl);
        submission.setStatus(0);
        submission.setTotalScore(null);
        submission.setSubmitTime(now);
        submission.setGradedTime(null);
        submission.setUpdatedTime(now);
        if (existing == null) {
            submissionMapper.insert(submission);
        } else {
            submissionMapper.updateById(submission);
        }

        processAsync(submission.getId(), imagePath);
        return enrichSubmission(submission);
    }

    @Async
    public void processAsync(Long submissionId, String imagePath) {
        try {
            updateStatus(submissionId, 1);
            Map<Integer, String> ocrTexts = ocrService.recognize(new File(imagePath));
            if (ocrTexts.isEmpty() && mockWhenOcrUnavailable) {
                ocrTexts = buildDemoOcrTexts(submissionId);
                log.warn("OCR服务不可用或识别为空，使用演示识别文本 submissionId={}", submissionId);
            } else {
                ocrTexts = alignOcrTexts(submissionId, ocrTexts);
            }
            updateStatus(submissionId, 2);
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
        s.setUpdatedTime(LocalDateTime.now());
        submissionMapper.updateById(s);
    }

    private void clearGradingResults(Long submissionId) {
        gradingResultMapper.delete(new LambdaQueryWrapper<com.homework.entity.GradingResult>()
                .eq(com.homework.entity.GradingResult::getSubmissionId, submissionId));
    }

    private Map<Integer, String> buildDemoOcrTexts(Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        Map<Integer, String> texts = new LinkedHashMap<>();
        if (submission == null) return texts;

        for (QuestionItem question : listQuestions(submission.getAssignmentId())) {
            String answer = question.getAnswerKey();
            if (question.getGradingMode() != null && question.getGradingMode() == 2
                    && question.getKeywords() != null && !question.getKeywords().isBlank()) {
                answer = question.getKeywords()
                        .replace("[", "")
                        .replace("]", "")
                        .replace("\"", "")
                        .replace(",", " ");
            }
            texts.put(question.getQuestionNo(), answer);
        }
        return texts;
    }

    private Map<Integer, String> alignOcrTexts(Long submissionId, Map<Integer, String> rawTexts) {
        Submission submission = submissionMapper.selectById(submissionId);
        Map<Integer, String> aligned = new LinkedHashMap<>();
        if (submission == null || rawTexts == null || rawTexts.isEmpty()) return aligned;

        List<QuestionItem> questions = listQuestions(submission.getAssignmentId());
        for (QuestionItem question : questions) {
            aligned.put(question.getQuestionNo(), "");
        }

        List<String> lines = rawTexts.values().stream()
                .filter(text -> text != null && !text.isBlank())
                .map(String::trim)
                .toList();

        for (int i = 0; i < questions.size(); i++) {
            QuestionItem question = questions.get(i);
            Integer nextQuestionNo = i + 1 < questions.size() ? questions.get(i + 1).getQuestionNo() : null;
            String answer = extractByQuestionMarker(lines, question, nextQuestionNo);
            if (answer.isBlank()) {
                answer = findLineByAnswerHint(lines, question);
            }
            aligned.put(question.getQuestionNo(), answer);
        }

        log.info("OCR对齐完成 submissionId={} raw={} aligned={}", submissionId, rawTexts, aligned);
        return aligned;
    }

    private List<QuestionItem> listQuestions(Long assignmentId) {
        return questionItemMapper.selectList(
                new LambdaQueryWrapper<QuestionItem>()
                        .eq(QuestionItem::getAssignmentId, assignmentId)
                        .orderByAsc(QuestionItem::getQuestionNo)
        );
    }

    private String extractByQuestionMarker(List<String> lines, QuestionItem question, Integer nextQuestionNo) {
        int start = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lineStartsWithQuestionNo(lines.get(i), question.getQuestionNo())) {
                start = i;
                break;
            }
        }
        if (start < 0) return "";

        StringBuilder segment = new StringBuilder();
        for (int i = start; i < lines.size(); i++) {
            String line = lines.get(i);
            if (i > start && nextQuestionNo != null && lineStartsWithQuestionNo(line, nextQuestionNo)) {
                break;
            }
            if (isNoiseLine(line)) continue;
            if (!segment.isEmpty()) segment.append(' ');
            segment.append(line);
        }
        return cleanAnswerText(segment.toString(), question);
    }

    private boolean lineStartsWithQuestionNo(String line, Integer questionNo) {
        if (line == null || questionNo == null) return false;
        String text = line.trim();
        Pattern pattern = Pattern.compile("^\\s*(?:第\\s*)?" + questionNo + "\\s*(?:题)?\\s*[.．、:：)]?.*");
        return pattern.matcher(text).matches();
    }

    private String findLineByAnswerHint(List<String> lines, QuestionItem question) {
        for (String line : lines) {
            if (isNoiseLine(line)) continue;
            String cleaned = cleanAnswerText(line, question);
            if (cleaned.isBlank()) continue;

            if (question.getQuestionType() != null && question.getQuestionType() == 1
                    && cleaned.matches(".*[A-Da-d].*")) {
                return cleaned;
            }

            String answerKey = normalizeForMatch(question.getAnswerKey());
            if (!answerKey.isBlank() && normalizeForMatch(cleaned).contains(answerKey)) {
                return cleaned;
            }

            if (question.getKeywords() != null) {
                String keywords = question.getKeywords()
                        .replace("[", "")
                        .replace("]", "")
                        .replace("\"", "");
                for (String keyword : keywords.split(",")) {
                    if (!keyword.isBlank() && normalizeForMatch(cleaned).contains(normalizeForMatch(keyword))) {
                        return cleaned;
                    }
                }
            }
        }
        return "";
    }

    private String cleanAnswerText(String text, QuestionItem question) {
        if (text == null) return "";
        String cleaned = text
                .replaceAll("\\s+", " ")
                .replaceAll("^\\s*(?:第\\s*)?\\d+\\s*(?:题)?\\s*[.．、:：)]?\\s*", "")
                .replaceAll("^(选择题|填空题|简答题|问答题|答案|答|题目)\\s*[:：]?\\s*", "")
                .trim();

        if (isNoiseLine(cleaned)) return "";

        if (question.getQuestionType() != null && question.getQuestionType() == 1) {
            Matcher matcher = Pattern.compile("[A-Da-d]").matcher(cleaned);
            return matcher.find() ? matcher.group().toUpperCase() : cleaned;
        }
        return cleaned;
    }

    private boolean isNoiseLine(String line) {
        if (line == null) return true;
        String text = line.trim();
        if (text.isBlank()) return true;
        return text.matches(".*(数学周测|语文阅读|作业|姓名|班级|学科|提示|保持图片|JPG|PNG|学生手写|页脚).*");
    }

    private String normalizeForMatch(String text) {
        if (text == null) return "";
        return text.trim()
                .toUpperCase()
                .replaceAll("[\\s　:：,，.。;；、()（）\\[\\]【】\"']", "");
    }

    public Page<Submission> listByStudent(Long studentId, Integer page, Integer size) {
        Page<Submission> result = submissionMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getStudentId, studentId)
                        .orderByDesc(Submission::getSubmitTime)
        );
        result.getRecords().forEach(this::enrichSubmission);
        return result;
    }

    public Page<Submission> listByAssignment(Long assignmentId, Integer page, Integer size) {
        Page<Submission> result = submissionMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getAssignmentId, assignmentId)
                        .orderByDesc(Submission::getSubmitTime)
        );
        result.getRecords().forEach(this::enrichSubmission);
        return result;
    }

    public Page<Submission> listByTeacher(Long teacherId, Integer page, Integer size) {
        Page<Submission> result = submissionMapper.selectTeacherSubmissions(
                new Page<>(page, size),
                teacherId
        );
        result.getRecords().forEach(this::enrichSubmission);
        return result;
    }

    public Submission getById(Long id) {
        return enrichSubmission(submissionMapper.selectById(id));
    }

    public List<Map<String, Object>> listWrongQuestions(Long studentId) {
        return submissionMapper.selectWrongQuestions(studentId);
    }

    private Submission enrichSubmission(Submission submission) {
        if (submission == null || submission.getAssignmentId() == null) return submission;
        var assignment = assignmentMapper.selectById(submission.getAssignmentId());
        if (assignment != null) {
            submission.setAssignmentTitle(assignment.getTitle());
            submission.setSubject(assignment.getSubject());
        }
        return submission;
    }

    private String saveImage(MultipartFile file, Long assignmentId, Long studentId) throws IOException {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Path dir = Paths.get(uploadPath).toAbsolutePath().normalize().resolve(String.valueOf(assignmentId));
        Files.createDirectories(dir);

        String originalFilename = file.getOriginalFilename();
        String suffix = ".jpg";
        if (originalFilename != null && originalFilename.contains(".")) {
            String candidate = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            if (candidate.matches("\\.(jpg|jpeg|png|webp)")) {
                suffix = candidate;
            }
        }

        String filename = studentId + "_" + dateStr + "_" + UUID.randomUUID().toString().substring(0, 8) + suffix;
        Path fullPath = dir.resolve(filename);
        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, fullPath);
        }
        return fullPath.toString();
    }
}
