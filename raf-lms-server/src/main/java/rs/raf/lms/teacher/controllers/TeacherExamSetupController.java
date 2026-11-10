package rs.raf.lms.teacher.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.raf.lms.teacher.dto.request.CreateExamGroupRequest;
import rs.raf.lms.teacher.dto.request.CreateExamPeriodRequest;
import rs.raf.lms.teacher.dto.request.CreateExamSessionRequest;
import rs.raf.lms.teacher.dto.response.ExamGroupResponse;
import rs.raf.lms.teacher.dto.response.ExamPeriodResponse;
import rs.raf.lms.teacher.dto.response.TeacherExamSessionResponse;
import rs.raf.lms.teacher.services.TeacherExamSetupService;

import java.util.List;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherExamSetupController {

    private final TeacherExamSetupService setupService;

    //rokovi
    @GetMapping("/exam-periods")
    public List<ExamPeriodResponse> getPeriods() {
        return setupService.getPeriods();
    }

    @PostMapping("/exam-periods")
    @ResponseStatus(HttpStatus.CREATED)
    public ExamPeriodResponse createPeriod(@Valid @RequestBody CreateExamPeriodRequest request) {
        return setupService.createPeriod(request);
    }

    //termini (lista već postoji u TeacherExamSessionController)
    @PostMapping("/courses/{courseId}/exam-sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public TeacherExamSessionResponse createSession(
            @PathVariable Long courseId,
            @Valid @RequestBody CreateExamSessionRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return setupService.createSession(jwt.getSubject(), courseId, request);
    }

    //grupe
    @GetMapping("/courses/{courseId}/exam-sessions/{sessionId}/groups")
    public List<ExamGroupResponse> getGroups(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return setupService.getGroups(jwt.getSubject(), courseId, sessionId);
    }

    @PostMapping("/courses/{courseId}/exam-sessions/{sessionId}/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public ExamGroupResponse createGroup(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @Valid @RequestBody CreateExamGroupRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return setupService.createGroup(jwt.getSubject(), courseId, sessionId, request);
    }
}