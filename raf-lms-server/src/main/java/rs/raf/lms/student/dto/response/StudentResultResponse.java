package rs.raf.lms.student.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record StudentResultResponse(
        Long sessionId,
        BigDecimal maxPoints,
        boolean graded,              // false dok nastavnik ne unese ocenu
        BigDecimal points,           // null dok nije ocenjeno
        String feedback,
        LocalDateTime gradedAt,
        List<SubmissionResult> submissions
) {
    public record SubmissionResult(
            Long submissionId,
            Long assignmentId,
            String assignmentTitle,
            String fileName,
            LocalDateTime submittedAt,
            List<CommentResult> comments
    ) {
    }

    public record CommentResult(
            Long id,
            String filePath,
            int lineNumber,
            String content,
            LocalDateTime createdAt
    ) {
    }
}