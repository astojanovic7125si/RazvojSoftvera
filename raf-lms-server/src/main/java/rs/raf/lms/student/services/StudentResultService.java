package rs.raf.lms.student.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.lms.exceptions.ResourceNotFoundException;
import rs.raf.lms.model.ExamAttempt;
import rs.raf.lms.model.Grade;
import rs.raf.lms.model.Submission;
import rs.raf.lms.model.SubmissionKind;
import rs.raf.lms.repositories.CodeCommentRepository;
import rs.raf.lms.repositories.ExamAttemptRepository;
import rs.raf.lms.repositories.GradeRepository;
import rs.raf.lms.repositories.SubmissionRepository;
import rs.raf.lms.student.dto.response.StudentResultResponse;
import rs.raf.lms.student.dto.response.StudentResultResponse.CommentResult;
import rs.raf.lms.student.dto.response.StudentResultResponse.SubmissionResult;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentResultService {

    private final ExamAttemptRepository examAttemptRepository;
    private final GradeRepository gradeRepository;
    private final SubmissionRepository submissionRepository;
    private final CodeCommentRepository codeCommentRepository;

    public StudentResultResponse getResult(String username, Long sessionId) {
        // Pokušaj se traži po korisničkom imenu iz tokena, pa student vidi samo svoj
        ExamAttempt attempt = examAttemptRepository
                .findByStudentUsernameAndSessionId(username, sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Za ovaj termin nemate rezultat"));

        BigDecimal maxPoints = attempt.getExamRegistration().getExamSession().getMaxPoints();

        Optional<Grade> grade = gradeRepository.findByExamAttemptId(attempt.getId());

        // Dok nastavnik ne oceni, student ne vidi ni poene ni komentare
        if (grade.isEmpty()) {
            return new StudentResultResponse(
                    sessionId, maxPoints, false, null, null, null, List.of());
        }

        return new StudentResultResponse(
                sessionId,
                maxPoints,
                true,
                grade.get().getPoints(),
                grade.get().getFeedback(),
                grade.get().getGradedAt(),
                buildSubmissionResults(attempt.getId()));
    }

    // Za svaki zadatak: poslednja FINAL predaja + komentari na nju
    private List<SubmissionResult> buildSubmissionResults(Long attemptId) {
        Map<Long, Submission> latestPerAssignment = new LinkedHashMap<>();

        for (Submission submission
                : submissionRepository.findByAttemptAndKind(attemptId, SubmissionKind.FINAL)) {
            latestPerAssignment.putIfAbsent(submission.getAssignment().getId(), submission);
        }

        return latestPerAssignment.values().stream()
                .map(this::toSubmissionResult)
                .toList();
    }

    private SubmissionResult toSubmissionResult(Submission submission) {
        List<CommentResult> comments = codeCommentRepository
                .findBySubmissionIdOrderByCreatedAtAsc(submission.getId())
                .stream()
                .map(comment -> new CommentResult(
                        comment.getId(),
                        comment.getFilePath(),
                        comment.getLineNumber(),
                        comment.getContent(),
                        comment.getCreatedAt()))
                .toList();

        return new SubmissionResult(
                submission.getId(),
                submission.getAssignment().getId(),
                submission.getAssignment().getTitle(),
                submission.getOriginalFileName(),
                submission.getSubmittedAt(),
                comments);
    }
}