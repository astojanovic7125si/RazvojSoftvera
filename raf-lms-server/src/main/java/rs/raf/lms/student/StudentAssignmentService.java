package rs.raf.lms.student;

import lombok.RequiredArgsConstructor;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import rs.raf.lms.storage.ProjectStorageService;
import rs.raf.lms.student.dto.StudentAssignmentResponse;
import rs.raf.lms.user.Student;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentAssignmentService {

    private final StudentRepository studentRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamRegistrationRepository examRegistrationRepository;
    private final AssignmentRepository assignmentRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ProjectStorageService projectStorageService;

    // PREGLED ZADATAKA DOSTUPNIH STUDENTU
    public List<StudentAssignmentResponse> getAssignments(
            String username,
            Long sessionId
    ) {

        ExamRegistration registration =
                requireActiveRegistration(username, sessionId);

        Long groupId = registration.getExamGroup() == null
                ? null
                : registration.getExamGroup().getId();

        return assignmentRepository
                .findForStudent(sessionId, groupId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // PREUZIMANJE ZIP PROJEKTA
    @Transactional
    public StudentProjectFile downloadProject(
            String username,
            Long sessionId,
            Long assignmentId
    ) {

        // 1. Provera prijave i vremena polaganja
        ExamRegistration registration =
                requireActiveRegistration(username, sessionId);

        // 2. Pronalazenje zadatka
        Assignment assignment = assignmentRepository
                .findByIdAndExamSessionId(assignmentId, sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Zadatak ne postoji u ovom terminu"
                ));

        // 3. Provera grupe
        if (assignment.getExamGroup() != null &&
                (registration.getExamGroup() == null ||
                        !assignment.getExamGroup().getId()
                                .equals(registration.getExamGroup().getId()))) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Zadatak nije namenjen tvojoj grupi"
            );
        }

        // 4. Provera da li je nastavnik postavio ZIP
        if (assignment.getStorageKey() == null) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Za ovaj zadatak jos nije postavljen ZIP projekat"
            );
        }

        // 5. Ucitavanje ZIP fajla
        Resource resource = projectStorageService
                .loadZip(assignment.getStorageKey());

        // 6. Evidencija prvog preuzimanja zadatka
        if (examAttemptRepository
                .findByExamRegistrationId(registration.getId())
                .isEmpty()) {

            examAttemptRepository.save(
                    new ExamAttempt(registration, LocalDateTime.now())
            );
        }

        return new StudentProjectFile(
                resource,
                assignment.getOriginalFileName()
        );
    }

    // PROVERA DA LI STUDENT SME DA PRISTUPI TERMINU
    private ExamRegistration requireActiveRegistration(
            String username,
            Long sessionId
    ) {

        // 1. Pronalazenje studenta na osnovu JWT identiteta
        Student student = studentRepository
                .findByUserUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Korisnik nema studentski profil"
                ));

        // 2. Provera da li termin postoji
        ExamSession session = examSessionRepository
                .findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ispitni termin ne postoji"
                ));

        // 3. Provera da li je student prijavljen
        ExamRegistration registration = examRegistrationRepository
                .findByStudentIdAndExamSessionId(
                        student.getId(),
                        sessionId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Student nije prijavljen za ovaj termin"
                ));

        // 4. Provera vremena polaganja
        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(session.getStartTime()) ||
                !now.isBefore(session.getEndTime())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Preuzimanje je dozvoljeno samo tokom ispita"
            );
        }

        return registration;
    }

    // PRETVARANJE ENTITETA U DTO
    private StudentAssignmentResponse toResponse(
            Assignment assignment
    ) {

        return new StudentAssignmentResponse(
                assignment.getId(),
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getExamGroup() == null
                        ? null
                        : assignment.getExamGroup().getId(),
                assignment.getOriginalFileName()
        );
    }

    public record StudentProjectFile(
            Resource resource,
            String originalFileName
    ) {
    }
}