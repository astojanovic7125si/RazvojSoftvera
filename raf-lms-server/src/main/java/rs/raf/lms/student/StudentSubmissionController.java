package rs.raf.lms.student;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.multipart.MultipartFile;

import rs.raf.lms.student.dto.StudentSubmissionResponse;

@RestController
@RequestMapping(
        "/api/student/exam-sessions/{sessionId}/assignments/{assignmentId}/submissions"
)
@RequiredArgsConstructor
public class StudentSubmissionController {

    private final StudentSubmissionService studentSubmissionService;

    // KONACNA PREDAJA RESENJA
    @PostMapping(
            value = "/final",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public StudentSubmissionResponse submitFinal(
            @PathVariable Long sessionId,
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("file") MultipartFile file
    ) {

        return studentSubmissionService.submitFinal(
                jwt.getSubject(),
                sessionId,
                assignmentId,
                file
        );
    }
}