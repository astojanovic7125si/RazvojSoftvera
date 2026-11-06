package rs.raf.lms.teacher;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/teacher/courses/{courseId}/exam-sessions/{sessionId}/assignments")
@RequiredArgsConstructor
public class TeacherAssignmentController {

    private final TeacherAssignmentService teacherAssignmentService;

    // UPLOAD PRIPREMLJENOG SOFTVERSKOG PROJEKTA U ZIP FORMATU
    @PostMapping(value = "/{assignmentId}/project", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TeacherAssignmentResponse uploadProject(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("file") MultipartFile file
    ) {
        return teacherAssignmentService.uploadProject(
                jwt.getSubject(), courseId, sessionId, assignmentId, file
        );
    }

    // PREUZIMANJE ZIP-A KOJI JE NASTAVNIK POSTAVIO
    @GetMapping("/{assignmentId}/project")
    public ResponseEntity<Resource> downloadProject(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        TeacherAssignmentService.ProjectFile project = teacherAssignmentService.downloadProject(
                jwt.getSubject(), courseId, sessionId, assignmentId
        );

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(project.originalFileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(project.resource());
    }

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
