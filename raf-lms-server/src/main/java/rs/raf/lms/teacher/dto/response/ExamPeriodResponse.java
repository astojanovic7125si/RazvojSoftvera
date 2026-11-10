package rs.raf.lms.teacher.dto.response;

import java.time.LocalDate;

public record ExamPeriodResponse(Long id, String name, LocalDate startDate, LocalDate endDate) {
}