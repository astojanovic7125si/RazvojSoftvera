package rs.raf.lms.teacher.dto.response;

import java.time.LocalDateTime;

public record SubmissionSummaryResponse(
        Long submissionId,
        Long attemptId,
        Long sessionId,
        String periodName,
        String indexNumber,
        String firstName,
        String lastName,
        String groupName,
        String classroom,
        Long assignmentId,
        String assignmentTitle,
        String fileName,
        LocalDateTime submittedAt
) {
}
