package rs.raf.lms.teacher.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import rs.raf.lms.teacher.dto.request.CreateCommentRequest;
import rs.raf.lms.teacher.dto.request.UpdateCommentRequest;
import rs.raf.lms.teacher.dto.response.CommentResponse;
import rs.raf.lms.teacher.services.TeacherCommentService;

import java.util.List;

@RestController
@RequestMapping("/api/teacher/courses/{courseId}/submissions/{submissionId}")
@RequiredArgsConstructor
public class TeacherCommentController {

    private final TeacherCommentService teacherCommentService;

    @GetMapping("/files")
    public List<String> getFiles(
            @PathVariable Long courseId,
            @PathVariable Long submissionId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return teacherCommentService.getFiles(jwt.getSubject(), courseId, submissionId);
    }

    @GetMapping("/comments")
    public List<CommentResponse> getComments(
            @PathVariable Long courseId,
            @PathVariable Long submissionId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return teacherCommentService.getComments(jwt.getSubject(), courseId, submissionId);
    }

    @PostMapping("/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse addComment(
            @PathVariable Long courseId,
            @PathVariable Long submissionId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return teacherCommentService.addComment(jwt.getSubject(), courseId, submissionId, request);
    }

    @PutMapping("/comments/{commentId}")
    public CommentResponse updateComment(
            @PathVariable Long courseId,
            @PathVariable Long submissionId,
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return teacherCommentService.updateComment(
                jwt.getSubject(), courseId, submissionId, commentId, request);
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @PathVariable Long courseId,
            @PathVariable Long submissionId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        teacherCommentService.deleteComment(jwt.getSubject(), courseId, submissionId, commentId);
    }
}
