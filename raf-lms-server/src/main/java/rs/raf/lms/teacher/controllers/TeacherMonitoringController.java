package rs.raf.lms.teacher.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.raf.lms.teacher.dto.response.MonitoringResponse;
import rs.raf.lms.teacher.services.TeacherMonitoringService;

@RestController
@RequestMapping("/api/teacher/courses/{courseId}/exam-sessions/{sessionId}/monitoring")
@RequiredArgsConstructor
public class TeacherMonitoringController {

    private final TeacherMonitoringService teacherMonitoringService;

    @GetMapping
    public MonitoringResponse getMonitoring(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return teacherMonitoringService.getMonitoring(jwt.getSubject(), courseId, sessionId);
    }
}
