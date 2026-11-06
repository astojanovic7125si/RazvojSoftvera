package rs.raf.lms.teacher;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import rs.raf.lms.teacher.dto.TeacherExamSessionResponse;

import java.util.List;

@RestController
@RequestMapping("/api/teacher/courses")
@RequiredArgsConstructor
public class TeacherExamSessionController {

    private final TeacherExamSessionService teacherExamSessionService;

    @GetMapping("/{courseId}/exam-sessions")
    public List<TeacherExamSessionResponse> getExamSessions(
            @PathVariable Long courseId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return teacherExamSessionService.getExamSessions(
                jwt.getSubject(),
                courseId
        );
    }
}