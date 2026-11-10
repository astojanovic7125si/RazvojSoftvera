package rs.raf.lms.teacher.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.raf.lms.teacher.dto.request.ImportRegistrationsRequest;
import rs.raf.lms.teacher.dto.response.ImportRegistrationsResponse;
import rs.raf.lms.teacher.dto.response.RegistrationResponse;
import rs.raf.lms.teacher.services.TeacherRegistrationService;

import java.util.List;

@RestController
@RequestMapping("/api/teacher/courses/{courseId}/exam-sessions/{sessionId}/registrations")
@RequiredArgsConstructor
public class TeacherRegistrationController {

    private final TeacherRegistrationService teacherRegistrationService;

    @PostMapping
    public ImportRegistrationsResponse importRegistrations(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @Valid @RequestBody ImportRegistrationsRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return teacherRegistrationService.importRegistrations(
                jwt.getSubject(), courseId, sessionId, request);
    }

    @GetMapping
    public List<RegistrationResponse> getRegistrations(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return teacherRegistrationService.getRegistrations(
                jwt.getSubject(), courseId, sessionId);
    }
}