package rs.raf.lms.teacher.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import rs.raf.lms.teacher.dto.request.GradeRequest;
import rs.raf.lms.teacher.dto.response.GradeResponse;
import rs.raf.lms.teacher.services.TeacherGradeService;

@RestController
@RequestMapping("/api/teacher/courses/{courseId}/attempts/{attemptId}/grade")
@RequiredArgsConstructor
public class TeacherGradeController {

    private final TeacherGradeService teacherGradeService;

    @GetMapping
    public GradeResponse getGrade(
            @PathVariable Long courseId,
            @PathVariable Long attemptId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return teacherGradeService.getGrade(jwt.getSubject(), courseId, attemptId);
    }

    @PutMapping
    public GradeResponse saveGrade(
            @PathVariable Long courseId,
            @PathVariable Long attemptId,
            @Valid @RequestBody GradeRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return teacherGradeService.saveGrade(jwt.getSubject(), courseId, attemptId, request);
    }
}