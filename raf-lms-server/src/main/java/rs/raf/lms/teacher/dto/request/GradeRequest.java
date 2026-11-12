package rs.raf.lms.teacher.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record GradeRequest(
        @NotNull @DecimalMin("0.00") @Digits(integer = 5, fraction = 2) BigDecimal points,
        @Size(max = 5000) String feedback
) {
}
