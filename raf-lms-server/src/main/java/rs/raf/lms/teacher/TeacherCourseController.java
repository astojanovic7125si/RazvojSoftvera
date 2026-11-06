package rs.raf.lms.teacher;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import rs.raf.lms.teacher.dto.TeacherCourseResponse;

import java.util.List;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherCourseController {

    private final TeacherCourseService teacherCourseService;

    @GetMapping("/courses")
    public List<TeacherCourseResponse> getMyCourses(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return teacherCourseService.getMyCourses(jwt.getSubject());
    }
}