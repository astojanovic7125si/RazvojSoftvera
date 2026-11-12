package rs.raf.lms.student.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.raf.lms.student.dto.response.StudentResultResponse;
import rs.raf.lms.student.services.StudentResultService;

@RestController
@RequestMapping("/api/student/exam-sessions/{sessionId}/result")
@RequiredArgsConstructor
public class StudentResultController {

    private final StudentResultService studentResultService;

    @GetMapping
    public StudentResultResponse getResult(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return studentResultService.getResult(jwt.getSubject(), sessionId);
    }
}
