package rs.raf.lms.teacher.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import rs.raf.lms.teacher.dto.response.SubmissionSummaryResponse;
import rs.raf.lms.teacher.services.TeacherSubmissionService;
import rs.raf.lms.teacher.services.TeacherSubmissionService.DownloadFile;
import rs.raf.lms.teacher.services.TeacherSubmissionService.Filter;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/teacher/courses/{courseId}/submissions")
@RequiredArgsConstructor
public class TeacherSubmissionController {

    private final TeacherSubmissionService teacherSubmissionService;

    @GetMapping
    public List<SubmissionSummaryResponse> getSubmissions(
            @PathVariable Long courseId,
            @RequestParam(name = "examPeriodId", required = false) Long examPeriodId,
            @RequestParam(name = "sessionId", required = false) Long sessionId,
            @RequestParam(name = "groupId", required = false) Long groupId,
            @RequestParam(name = "classroom", required = false) String classroom,
            @RequestParam(name = "indexNumber", required = false) String indexNumber,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Filter filter = new Filter(examPeriodId, sessionId, groupId, classroom, indexNumber);
        return teacherSubmissionService.getSubmissions(jwt.getSubject(), courseId, filter);
    }

    @GetMapping("/{submissionId}/download")
    public ResponseEntity<Resource> downloadOne(
            @PathVariable Long courseId,
            @PathVariable Long submissionId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        DownloadFile file = teacherSubmissionService
                .downloadOne(jwt.getSubject(), courseId, submissionId);
        return zipResponse(file);
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadAll(
            @PathVariable Long courseId,
            @RequestParam(name = "examPeriodId", required = false) Long examPeriodId,
            @RequestParam(name = "sessionId", required = false) Long sessionId,
            @RequestParam(name = "groupId", required = false) Long groupId,
            @RequestParam(name = "classroom", required = false) String classroom,
            @RequestParam(name = "indexNumber", required = false) String indexNumber,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Filter filter = new Filter(examPeriodId, sessionId, groupId, classroom, indexNumber);
        DownloadFile file = teacherSubmissionService
                .downloadAll(jwt.getSubject(), courseId, filter);
        return zipResponse(file);
    }

    private ResponseEntity<Resource> zipResponse(DownloadFile file) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.resource());
    }
}
