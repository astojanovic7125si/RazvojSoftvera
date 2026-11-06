package rs.raf.lms.teacher.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TeacherExamSessionResponse(
        Long id,
        Long examPeriodId,
        String examPeriodName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        BigDecimal maxPoints
) {
}