package rs.raf.lms.teacher;

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

import rs.raf.lms.teacher.dto.CreateAssignmentRequest;
import rs.raf.lms.teacher.dto.TeacherAssignmentResponse;
import org.springframework.web.bind.annotation.PutMapping;
import rs.raf.lms.teacher.dto.UpdateAssignmentRequest;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.util.List;

@RestController
@RequestMapping("/api/teacher/courses/{courseId}/exam-sessions/{sessionId}/assignments")
@RequiredArgsConstructor
public class TeacherAssignmentController {

    private final TeacherAssignmentService teacherAssignmentService;

    // KREIRANJE ZADATKA
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeacherAssignmentResponse createAssignment(

            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateAssignmentRequest request

    ) {

        return teacherAssignmentService.createAssignment(
                jwt.getSubject(),
                courseId,
                sessionId,
                request
        );
    }

    // PREGLED ZADATAKA
    @GetMapping
    public List<TeacherAssignmentResponse> getAssignments(

            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @AuthenticationPrincipal Jwt jwt

    ) {

        return teacherAssignmentService.getAssignments(
                jwt.getSubject(),
                courseId,
                sessionId
        );
    }

    // IZMENA ZADATKA
    @PutMapping("/{assignmentId}")
    public TeacherAssignmentResponse updateAssignment(

            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @PathVariable Long assignmentId,

            @AuthenticationPrincipal Jwt jwt,

            @Valid @RequestBody UpdateAssignmentRequest request

    ) {

        return teacherAssignmentService.updateAssignment(
                jwt.getSubject(),
                courseId,
                sessionId,
                assignmentId,
                request
        );
    }

    // BRISANJE ZADATKA
    @DeleteMapping("/{assignmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAssignment(

            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @PathVariable Long assignmentId,

            @AuthenticationPrincipal Jwt jwt

    ) {

        teacherAssignmentService.deleteAssignment(
                jwt.getSubject(),
                courseId,
                sessionId,
                assignmentId
        );
    }
}