package rs.raf.lms.student.dto.response;

import java.time.LocalDateTime;

public record StudentSubmissionResponse(
        Long id,
        Long assignmentId,
        String kind,
        String fileName,
        LocalDateTime submittedAt
) {
}