package rs.raf.lms.student;

import lombok.RequiredArgsConstructor;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import rs.raf.lms.student.dto.StudentAssignmentResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/student/exam-sessions/{sessionId}/assignments")
@RequiredArgsConstructor
public class StudentAssignmentController {

    private final StudentAssignmentService studentAssignmentService;

    // PRIKAZ ZADATAKA STUDENTA
    @GetMapping
    public List<StudentAssignmentResponse> getAssignments(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal Jwt jwt
    ) {

        return studentAssignmentService.getAssignments(
                jwt.getSubject(),
                sessionId
        );
    }

    // PREUZIMANJE ZIP PROJEKTA
    @GetMapping("/{assignmentId}/project")
    public ResponseEntity<Resource> downloadProject(
            @PathVariable Long sessionId,
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal Jwt jwt
    ) {

        StudentAssignmentService.StudentProjectFile project =
                studentAssignmentService.downloadProject(
                        jwt.getSubject(),
                        sessionId,
                        assignmentId
                );

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(
                        project.originalFileName(),
                        StandardCharsets.UTF_8
                )
                .build();

        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType("application/zip")
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString()
                )
                .body(project.resource());
    }
}