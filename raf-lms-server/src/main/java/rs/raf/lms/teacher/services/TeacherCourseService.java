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
import rs.raf.lms.teacher.dto.response.TeacherCourseResponse;
import rs.raf.lms.model.AppUser;
import rs.raf.lms.model.Role;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherCourseService {

    private final AppUserRepository appUserRepository;
    private final CourseRepository courseRepository;

    public List<TeacherCourseResponse> getMyCourses(String username) {

        AppUser teacher = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Korisnik ne postoji"
                ));

        if (teacher.getRole() != Role.TEACHER) {
            throw new ForbiddenException("Pristup je dozvoljen samo nastavnicima"
            );
        }

        return courseRepository.findByTeachers_Id(teacher.getId())
                .stream()
                .map(course -> new TeacherCourseResponse(
                        course.getId(),
                        course.getCode(),
                        course.getName()
                ))
                .toList();
    }
}