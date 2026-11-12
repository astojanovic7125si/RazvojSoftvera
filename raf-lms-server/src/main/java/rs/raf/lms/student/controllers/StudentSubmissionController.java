package rs.raf.lms.student.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import rs.raf.lms.student.dto.response.StudentSubmissionResponse;
import rs.raf.lms.student.services.StudentSubmissionService;

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
            //samo zahteve u formatu za upload fajlova, Ako je JSON, vraća 415
    )
    @ResponseStatus(HttpStatus.CREATED)
    //Umesto 200, uspešan ima 201 Created
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