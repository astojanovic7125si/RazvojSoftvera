package rs.raf.lms.teacher.dto.response;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        Long submissionId,
        String filePath,
        int lineNumber,
        String content,
        LocalDateTime createdAt
) {
}
