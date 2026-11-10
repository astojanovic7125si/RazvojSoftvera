package rs.raf.lms.teacher.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateExamSessionRequest(
        @NotNull Long examPeriodId,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime,
        @NotNull @Positive @Digits(integer = 5, fraction = 2) BigDecimal maxPoints
) {
}