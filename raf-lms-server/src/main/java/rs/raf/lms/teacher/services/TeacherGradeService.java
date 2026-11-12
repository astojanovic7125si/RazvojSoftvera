package rs.raf.lms.teacher.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.lms.exceptions.BadRequestException;
import rs.raf.lms.exceptions.ResourceNotFoundException;
import rs.raf.lms.exceptions.UnauthorizedException;
import rs.raf.lms.model.AppUser;
import rs.raf.lms.model.ExamAttempt;
import rs.raf.lms.model.Grade;
import rs.raf.lms.repositories.AppUserRepository;
import rs.raf.lms.repositories.ExamAttemptRepository;
import rs.raf.lms.repositories.GradeRepository;
import rs.raf.lms.teacher.dto.request.GradeRequest;
import rs.raf.lms.teacher.dto.response.GradeResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherGradeService {

    private final TeacherAccessService teacherAccessService;
    private final AppUserRepository appUserRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final GradeRepository gradeRepository;

    // PREGLED OCENE
    public GradeResponse getGrade(String username, Long courseId, Long attemptId) {
        ExamAttempt attempt = requireAttempt(username, courseId, attemptId);

        Grade grade = gradeRepository.findByExamAttemptId(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Ocena još nije uneta"));

        return toResponse(attempt, grade);
    }

    // UNOS ILI IZMENA OCENE
    @Transactional
    public GradeResponse saveGrade(
            String username, Long courseId, Long attemptId, GradeRequest request
    ) {
        ExamAttempt attempt = requireAttempt(username, courseId, attemptId);

        // Poeni ne smeju preko maksimuma koji je zadat za termin
        BigDecimal maxPoints = attempt.getExamRegistration().getExamSession().getMaxPoints();
        if (request.points().compareTo(maxPoints) > 0) {
            throw new BadRequestException(
                    "Poeni ne mogu biti veći od " + maxPoints.stripTrailingZeros().toPlainString());
        }

        AppUser teacher = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Korisnik ne postoji"));

        BigDecimal points = request.points().setScale(2, RoundingMode.HALF_UP);
        String feedback = (request.feedback() == null || request.feedback().isBlank())
                ? null
                : request.feedback().trim();
        LocalDateTime now = LocalDateTime.now();

        Optional<Grade> existing = gradeRepository.findByExamAttemptId(attemptId);
        Grade grade;

        if (existing.isPresent()) {
            // Ocena već postoji -> menjamo je (Hibernate sam upiše izmenu na kraju transakcije)
            grade = existing.get();
            grade.update(teacher, points, feedback, now);
        } else {
            grade = gradeRepository.save(new Grade(attempt, teacher, points, feedback, now));
        }

        return toResponse(attempt, grade);
    }

    // Prava nastavnika + pokušaj pripada ovom predmetu
    private ExamAttempt requireAttempt(String username, Long courseId, Long attemptId) {
        teacherAccessService.requireAuthorizedCourse(username, courseId);

        return examAttemptRepository.findByIdForCourse(attemptId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Pokušaj ne postoji"));
    }

    private GradeResponse toResponse(ExamAttempt attempt, Grade grade) {
        return new GradeResponse(
                attempt.getId(),
                grade.getPoints(),
                attempt.getExamRegistration().getExamSession().getMaxPoints(),
                grade.getFeedback(),
                grade.getGradedAt());
    }
}