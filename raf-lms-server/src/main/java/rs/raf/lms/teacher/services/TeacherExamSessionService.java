package rs.raf.lms.teacher.services;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import rs.raf.lms.exceptions.ForbiddenException;
import rs.raf.lms.exceptions.UnauthorizedException;
import rs.raf.lms.repositories.AppUserRepository;
import rs.raf.lms.repositories.CourseRepository;
import rs.raf.lms.repositories.ExamSessionRepository;

import rs.raf.lms.teacher.dto.response.TeacherExamSessionResponse;

import rs.raf.lms.model.AppUser;
import rs.raf.lms.model.Role;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherExamSessionService {

    private final AppUserRepository appUserRepository;
    private final CourseRepository courseRepository;
    private final ExamSessionRepository examSessionRepository;

    public List<TeacherExamSessionResponse> getExamSessions(
            String username,
            Long courseId
    ) {

        AppUser teacher = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Korisnik ne postoji"
                ));

        if (teacher.getRole() != Role.TEACHER) {
            throw new ForbiddenException("Pristup je dozvoljen samo nastavnicima"
            );
        }

        boolean teachesCourse =
                courseRepository.existsByIdAndTeachers_Id(
                        courseId,
                        teacher.getId()
                );

        if (!teachesCourse) {
            throw new ForbiddenException("Nastavnik ne predaje ovaj predmet"
            );
        }

        return examSessionRepository.findByCourseId(courseId)
                .stream()
                .map(session -> new TeacherExamSessionResponse(
                        session.getId(),
                        session.getExamPeriod().getId(),
                        session.getExamPeriod().getName(),
                        session.getStartTime(),
                        session.getEndTime(),
                        session.getMaxPoints()
                ))
                .toList();
    }
}