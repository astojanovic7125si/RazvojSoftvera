package rs.raf.lms.teacher.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.lms.exceptions.BadRequestException;
import rs.raf.lms.exceptions.ResourceNotFoundException;
import rs.raf.lms.exceptions.UnauthorizedException;
import rs.raf.lms.model.AppUser;
import rs.raf.lms.model.CodeComment;
import rs.raf.lms.model.Submission;
import rs.raf.lms.model.SubmissionKind;
import rs.raf.lms.repositories.AppUserRepository;
import rs.raf.lms.repositories.CodeCommentRepository;
import rs.raf.lms.repositories.SubmissionRepository;
import rs.raf.lms.storage.SubmissionStorageService;
import rs.raf.lms.storage.ZipInspector;
import rs.raf.lms.teacher.dto.request.CreateCommentRequest;
import rs.raf.lms.teacher.dto.request.UpdateCommentRequest;
import rs.raf.lms.teacher.dto.response.CommentResponse;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherCommentService {

    private final TeacherAccessService teacherAccessService;
    private final AppUserRepository appUserRepository;
    private final SubmissionRepository submissionRepository;
    private final CodeCommentRepository codeCommentRepository;
    private final SubmissionStorageService submissionStorageService;
    private final ZipInspector zipInspector;

    // SPISAK FAJLOVA U REŠENJU
    public List<String> getFiles(String username, Long courseId, Long submissionId) {
        Submission submission = requireSubmission(username, courseId, submissionId);

        return zipInspector.listFiles(
                submissionStorageService.loadZip(submission.getStorageKey()));
    }

    // SVI KOMENTARI JEDNE PREDAJE
    public List<CommentResponse> getComments(String username, Long courseId, Long submissionId) {
        requireSubmission(username, courseId, submissionId);

        return codeCommentRepository.findBySubmissionIdOrderByCreatedAtAsc(submissionId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // DODAVANJE KOMENTARA
    @Transactional
    public CommentResponse addComment(
            String username, Long courseId, Long submissionId, CreateCommentRequest request
    ) {
        Submission submission = requireSubmission(username, courseId, submissionId);

        if (submission.getKind() != SubmissionKind.FINAL) {
            throw new BadRequestException("Komentari se ostavljaju samo na konačnu predaju");
        }

        // Fajl mora da postoji u ZIP-u, a linija u fajlu
        int lineCount = zipInspector.countLines(
                submissionStorageService.loadZip(submission.getStorageKey()),
                request.filePath());

        if (request.lineNumber() > lineCount) {
            throw new BadRequestException("Fajl ima samo " + lineCount + " linija");
        }

        AppUser author = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Korisnik ne postoji"));

        CodeComment comment = new CodeComment(
                submission,
                author,
                zipInspector.normalize(request.filePath()),
                request.lineNumber(),
                request.content().trim(),
                LocalDateTime.now());

        return toResponse(codeCommentRepository.save(comment));
    }

    // IZMENA TEKSTA KOMENTARA
    @Transactional
    public CommentResponse updateComment(
            String username, Long courseId, Long submissionId,
            Long commentId, UpdateCommentRequest request
    ) {
        requireSubmission(username, courseId, submissionId);

        CodeComment comment = requireComment(commentId, submissionId);
        comment.changeContent(request.content().trim());   // Hibernate sam upiše izmenu na kraju transakcije

        return toResponse(comment);
    }

    // BRISANJE KOMENTARA
    @Transactional
    public void deleteComment(
            String username, Long courseId, Long submissionId, Long commentId
    ) {
        requireSubmission(username, courseId, submissionId);

        codeCommentRepository.delete(requireComment(commentId, submissionId));
    }

    // Prava nastavnika + predaja pripada tom predmetu
    private Submission requireSubmission(String username, Long courseId, Long submissionId) {
        teacherAccessService.requireAuthorizedCourse(username, courseId);

        return submissionRepository.findByIdForCourse(submissionId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Predaja ne postoji"));
    }

    private CodeComment requireComment(Long commentId, Long submissionId) {
        return codeCommentRepository.findByIdAndSubmissionId(commentId, submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Komentar ne postoji"));
    }

    private CommentResponse toResponse(CodeComment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getSubmission().getId(),
                comment.getFilePath(),
                comment.getLineNumber(),
                comment.getContent(),
                comment.getCreatedAt());
    }
}
