package rs.raf.lms.teacher.services;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.lms.exceptions.InternalServerException;
import rs.raf.lms.exceptions.ResourceNotFoundException;
import rs.raf.lms.model.ExamRegistration;
import rs.raf.lms.model.Submission;
import rs.raf.lms.model.SubmissionKind;
import rs.raf.lms.repositories.SubmissionRepository;
import rs.raf.lms.storage.SubmissionStorageService;
import rs.raf.lms.teacher.dto.response.SubmissionSummaryResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherSubmissionService {

    private final TeacherAccessService teacherAccessService;
    private final SubmissionRepository submissionRepository;
    private final SubmissionStorageService submissionStorageService;

    // Kriterijumi selekcije, polje koje je null se ne uzima u obzir.
    public record Filter(
            Long examPeriodId,
            Long sessionId,
            Long groupId,
            String classroom,
            String indexNumber
    ) {
    }

    public record DownloadFile(Resource resource, String fileName) {
    }

    // LISTA REŠENJA (poslednja FINAL predaja po studentu i zadatku)
    public List<SubmissionSummaryResponse> getSubmissions(
            String username, Long courseId, Filter filter
    ) {
        teacherAccessService.requireAuthorizedCourse(username, courseId);

        return findLatestFinal(courseId, filter).stream()
                .map(this::toResponse)
                .toList();
    }

    // PREUZIMANJE JEDNOG REŠENJA
    public DownloadFile downloadOne(String username, Long courseId, Long submissionId) {
        teacherAccessService.requireAuthorizedCourse(username, courseId);

        Submission submission = submissionRepository
                .findByIdForCourse(submissionId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Predaja ne postoji"));

        return new DownloadFile(
                submissionStorageService.loadZip(submission.getStorageKey()),
                fileName(submission)
        );
    }

    // PREUZIMANJE SVIH REŠENJA PO KRITERIJUMIMA: jedan ZIP u kome je svako rešenje ZIP za sebe
    public DownloadFile downloadAll(String username, Long courseId, Filter filter) {
        teacherAccessService.requireAuthorizedCourse(username, courseId);

        List<Submission> submissions = findLatestFinal(courseId, filter);
        if (submissions.isEmpty()) {
            throw new ResourceNotFoundException("Nema rešenja za zadate kriterijume");
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (Submission submission : submissions) {
                Long sessionId = submission.getExamAttempt()
                        .getExamRegistration().getExamSession().getId();

                // termin-2/2023_0000_zadatak-1-mnozenje.zip
                zip.putNextEntry(new ZipEntry("termin-" + sessionId + "/" + fileName(submission)));
                try (InputStream input = submissionStorageService
                        .loadZip(submission.getStorageKey()).getInputStream()) {
                    input.transferTo(zip);
                }
                zip.closeEntry();
            }
        } catch (IOException exception) {
            throw new InternalServerException("Neuspešno pravljenje ZIP arhive", exception);
        }

        return new DownloadFile(new ByteArrayResource(bytes.toByteArray()), "resenja.zip");
    }

    // Upit vraća predaje od najnovije ka najstarijoj za svaki par (pokušaj, zadatak),
    // pa za svaki par zadržavamo samo prvu, tj. najnoviju.
    private List<Submission> findLatestFinal(Long courseId, Filter filter) {
        List<Submission> all = submissionRepository.findForReview(
                courseId,
                SubmissionKind.FINAL,
                filter.examPeriodId(),
                filter.sessionId(),
                filter.groupId(),
                blankToNull(filter.classroom()),
                blankToNull(filter.indexNumber())
        );

        Map<String, Submission> latest = new LinkedHashMap<>();
        for (Submission submission : all) {
            String key = submission.getExamAttempt().getId()
                    + ":" + submission.getAssignment().getId();
            latest.putIfAbsent(key, submission);
        }
        return new ArrayList<>(latest.values());
    }

    private SubmissionSummaryResponse toResponse(Submission s) {
        ExamRegistration r = s.getExamAttempt().getExamRegistration();

        return new SubmissionSummaryResponse(
                s.getId(),
                s.getExamAttempt().getId(),
                r.getExamSession().getId(),
                r.getExamSession().getExamPeriod().getName(),
                r.getStudent().getIndexNumber(),
                r.getStudent().getUser().getFirstName(),
                r.getStudent().getUser().getLastName(),
                r.getExamGroup() == null ? null : r.getExamGroup().getName(),
                r.getClassroom(),
                s.getAssignment().getId(),
                s.getAssignment().getTitle(),
                s.getOriginalFileName(),
                s.getSubmittedAt()
        );
    }

    // npr. 2023_0000_zadatak-1-mnozenje.zip
    private String fileName(Submission s) {
        String index = safe(s.getExamAttempt().getExamRegistration()
                .getStudent().getIndexNumber());
        return index + "_zadatak-" + s.getAssignment().getId()
                + "-" + safe(s.getAssignment().getTitle()) + ".zip";
    }

    private String safe(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
