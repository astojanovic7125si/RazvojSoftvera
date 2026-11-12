package rs.raf.lms.student.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.raf.lms.student.dto.response.StudentExamSessionResponse;
import rs.raf.lms.student.services.StudentExamSessionService;

import java.util.List;

@RestController
@RequestMapping("/api/student/exam-sessions")
@RequiredArgsConstructor
public class StudentExamSessionController {

    private final StudentExamSessionService studentExamSessionService;

    @GetMapping
    public List<StudentExamSessionResponse> getMySessions(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return studentExamSessionService.getMySessions(jwt.getSubject());
    }
}