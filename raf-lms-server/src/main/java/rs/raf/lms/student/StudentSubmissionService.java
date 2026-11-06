package rs.raf.lms.student;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import rs.raf.lms.assignment.Assignment;

import rs.raf.lms.exam.ExamAttempt;
import rs.raf.lms.exam.ExamRegistration;
import rs.raf.lms.exam.ExamSession;

import rs.raf.lms.repositories.AssignmentRepository;
import rs.raf.lms.repositories.ExamAttemptRepository;
import rs.raf.lms.repositories.ExamRegistrationRepository;
import rs.raf.lms.repositories.ExamSessionRepository;
import rs.raf.lms.repositories.StudentRepository;
import rs.raf.lms.repositories.SubmissionRepository;

import rs.raf.lms.storage.SubmissionStorageService;
import rs.raf.lms.student.dto.StudentSubmissionResponse;

import rs.raf.lms.submission.Submission;
import rs.raf.lms.submission.SubmissionKind;
import rs.raf.lms.user.Student;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentSubmissionService {

    private final StudentRepository studentRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamRegistrationRepository examRegistrationRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final SubmissionStorageService submissionStorageService;

    // KONACNA PREDAJA STUDENTSKOG RESENJA
    public StudentSubmissionResponse submitFinal(
            String username,
            Long sessionId,
            Long assignmentId,
            MultipartFile file
    ) {

        // 1. Pronalazimo studenta
        Student student = studentRepository
                .findByUserUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Korisnik nema studentski profil"
                ));

        // 2. Pronalazimo ispitni termin
        ExamSession session = examSessionRepository
                .findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ispitni termin ne postoji"
                ));

        // 3. Proveravamo da li je student prijavljen
        ExamRegistration registration = examRegistrationRepository
                .findByStudentIdAndExamSessionId(
                        student.getId(),
                        sessionId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Student nije prijavljen za ovaj termin"
                ));

        // 4. Proveravamo rok za predaju
        ensureBeforeDeadline(session);

        // 5. Pronalazimo zadatak
        Assignment assignment = assignmentRepository
                .findByIdAndExamSessionId(assignmentId, sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Zadatak ne pripada ovom terminu"
                ));

        // 6. Proveravamo kojoj grupi zadatak pripada
        if (assignment.getExamGroup() != null &&
                (registration.getExamGroup() == null ||
                        !assignment.getExamGroup().getId()
                                .equals(registration.getExamGroup().getId()))) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Zadatak nije namenjen tvojoj grupi"
            );
        }

        // 7. Proveravamo da li postoji originalni zadatak
        if (assignment.getStorageKey() == null) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Nastavnik jos nije postavio ZIP zadatka"
            );
        }

        // 8. Student mora prethodno da zapocne polaganje
        ExamAttempt attempt = examAttemptRepository
                .findByExamRegistrationId(registration.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Prvo preuzmi zadatak, pa predaj resenje"
                ));

        // 9. Cuvamo ZIP studentskog resenja
        SubmissionStorageService.StoredSubmission stored =
                submissionStorageService.storeZip(file);

        // 10. Ponovo proveravamo rok, jer slanje fajla traje
        LocalDateTime submittedAt = LocalDateTime.now();

        if (!submittedAt.isBefore(session.getEndTime())) {

            submissionStorageService.deleteZip(
                    stored.storageKey()
            );

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Termin je istekao tokom slanja resenja"
            );
        }

        // 11. Cuvamo podatke o predaji u bazi
        Submission submission = submissionRepository.save(
                new Submission(
                        attempt,
                        assignment,
                        SubmissionKind.FINAL,
                        stored.originalFileName(),
                        stored.storageKey(),
                        submittedAt
                )
        );

        // 12. Vracamo potvrdu studentu
        return new StudentSubmissionResponse(
                submission.getId(),
                assignment.getId(),
                submission.getKind().name(),
                submission.getOriginalFileName(),
                submission.getSubmittedAt()
        );
    }

    // PROVERA VREMENSKOG OGRANICENJA
    private void ensureBeforeDeadline(ExamSession session) {

        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(session.getStartTime()) ||
                !now.isBefore(session.getEndTime())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Predaja je dozvoljena samo tokom ispita"
            );
        }
    }
}