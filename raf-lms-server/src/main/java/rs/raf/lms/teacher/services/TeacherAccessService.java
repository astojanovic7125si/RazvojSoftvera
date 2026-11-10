package rs.raf.lms.teacher.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.lms.exceptions.ForbiddenException;
import rs.raf.lms.exceptions.ResourceNotFoundException;
import rs.raf.lms.exceptions.UnauthorizedException;
import rs.raf.lms.model.AppUser;
import rs.raf.lms.model.Course;
import rs.raf.lms.model.ExamSession;
import rs.raf.lms.model.Role;
import rs.raf.lms.repositories.AppUserRepository;
import rs.raf.lms.repositories.CourseRepository;
import rs.raf.lms.repositories.ExamSessionRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherAccessService {

    private final AppUserRepository appUserRepository;
    private final CourseRepository courseRepository;
    private final ExamSessionRepository examSessionRepository;

    // Provera: korisnik postoji (401), nastavnik je (403), predaje taj predmet (403).
    public Course requireAuthorizedCourse(String username, Long courseId) {
        AppUser teacher = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Korisnik ne postoji"));

        if (teacher.getRole() != Role.TEACHER) {
            throw new ForbiddenException("Pristup je dozvoljen samo nastavnicima");
        }

        if (!courseRepository.existsByIdAndTeachers_Id(courseId, teacher.getId())) {
            throw new ForbiddenException("Nastavnik ne predaje ovaj predmet");
        }

        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Predmet ne postoji"));
    }

    // Sve gore + termin postoji i pripada tom predmetu (404).
    public ExamSession requireAuthorizedSession(String username, Long courseId, Long sessionId) {
        requireAuthorizedCourse(username, courseId);

        return examSessionRepository.findByIdAndCourseId(sessionId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Termin ne postoji"));
    }
}