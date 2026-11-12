package rs.raf.lms.student.dto.response;

import java.time.LocalDateTime;

public record StudentExamSessionResponse(
        Long sessionId,
        Long courseId,
        String courseCode,
        String courseName,
        String examPeriodName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String groupName,
        String classroom,
        Status status
) {
    public enum Status {
        UPCOMING,   // još nije počeo
        ACTIVE,     // traje
        FINISHED    // završen
    }
}