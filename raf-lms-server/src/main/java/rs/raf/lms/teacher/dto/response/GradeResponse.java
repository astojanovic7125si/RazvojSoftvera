package rs.raf.lms.teacher.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GradeResponse(
        Long attemptId,
        BigDecimal points,
        BigDecimal maxPoints,
        String feedback,
        LocalDateTime gradedAt
) {
}
