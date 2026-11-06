package rs.raf.lms.teacher;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import rs.raf.lms.assignment.Assignment;
import rs.raf.lms.exam.ExamGroup;
import rs.raf.lms.exam.ExamSession;

import rs.raf.lms.repositories.AppUserRepository;
import rs.raf.lms.repositories.AssignmentRepository;
import rs.raf.lms.repositories.CourseRepository;
import rs.raf.lms.repositories.ExamGroupRepository;
import rs.raf.lms.repositories.ExamSessionRepository;

import rs.raf.lms.teacher.dto.CreateAssignmentRequest;
import rs.raf.lms.teacher.dto.TeacherAssignmentResponse;
import rs.raf.lms.teacher.dto.UpdateAssignmentRequest;
import rs.raf.lms.repositories.SubmissionRepository;

import rs.raf.lms.user.AppUser;
import rs.raf.lms.user.Role;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TeacherAssignmentService {

    private final AppUserRepository appUserRepository;
    private final CourseRepository courseRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamGroupRepository examGroupRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;

    // KREIRANJE ZADATKA
    public TeacherAssignmentResponse createAssignment(
            String username,
            Long courseId,
            Long sessionId,
            CreateAssignmentRequest request
    ) {

        ExamSession session = requireAuthorizedSession(
                username,
                courseId,
                sessionId
        );

        ExamGroup group = null;

        if (request.examGroupId() != null) {
            group = examGroupRepository
                    .findByIdAndExamSessionId(
                            request.examGroupId(),
                            sessionId
                    )
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Ispitna grupa ne pripada ovom terminu"
                    ));
        }

        Assignment assignment = new Assignment(
                session,
                request.title().trim(),
                request.description()
        );

        if (group != null) {
            assignment.setExamGroup(group);
        }

        Assignment savedAssignment =
                assignmentRepository.save(assignment);

        return toResponse(savedAssignment);
    }

    // PREGLED ZADATAKA
    @Transactional(readOnly = true)
    public List<TeacherAssignmentResponse> getAssignments(
            String username,
            Long courseId,
            Long sessionId
    ) {

        requireAuthorizedSession(
                username,
                courseId,
                sessionId
        );

        return assignmentRepository.findByExamSessionId(sessionId)
                .stream()
                .map(this::toResponse)
                .toList();
    }


    // IZMENA POSTOJECEG ZADATKA
    public TeacherAssignmentResponse updateAssignment(
            String username,
            Long courseId,
            Long sessionId,
            Long assignmentId,
            UpdateAssignmentRequest request
    ) {

        // 1. Provera prava pristupa
        requireAuthorizedSession(
                username,
                courseId,
                sessionId
        );

        // 2. Pronalazimo zadatak unutar odgovarajuceg termina
        Assignment assignment = assignmentRepository
                .findByIdAndExamSessionId(assignmentId, sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Zadatak ne postoji u ovom terminu"
                ));

        // 3. Proveravamo grupu ako je navedena
        ExamGroup group = null;

        if (request.examGroupId() != null) {
            group = examGroupRepository
                    .findByIdAndExamSessionId(
                            request.examGroupId(),
                            sessionId
                    )
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Ispitna grupa ne pripada ovom terminu"
                    ));
        }

        // 4. Menjamo podatke zadatka
        assignment.setTitle(request.title().trim());
        assignment.setDescription(request.description());
        assignment.setExamGroup(group);

        // 5. Cuvamo izmene
        Assignment updatedAssignment =
                assignmentRepository.save(assignment);

        // 6. Vracamo izmenjeni zadatak
        return toResponse(updatedAssignment);
    }

    // BRISANJE ZADATKA
    public void deleteAssignment(
            String username,
            Long courseId,
            Long sessionId,
            Long assignmentId
    ) {

        // 1. Proveravamo prava pristupa
        requireAuthorizedSession(
                username,
                courseId,
                sessionId
        );

        // 2. Pronalazimo zadatak u odgovarajucem terminu
        Assignment assignment = assignmentRepository
                .findByIdAndExamSessionId(assignmentId, sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Zadatak ne postoji u ovom terminu"
                ));

        // 3. Proveravamo da li postoje studentske predaje
        boolean hasSubmissions =
                submissionRepository.existsByAssignment_Id(assignmentId);

        if (hasSubmissions) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Zadatak ne moze biti obrisan jer postoje studentske predaje"
            );
        }

        // 4. Brisemo zadatak
        assignmentRepository.delete(assignment);
    }

    // ZAJEDNICKA PROVERA PRISTUPA
    private ExamSession requireAuthorizedSession(
            String username,
            Long courseId,
            Long sessionId
    ) {

        AppUser teacher = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Korisnik ne postoji"
                ));

        if (teacher.getRole() != Role.TEACHER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Pristup je dozvoljen samo nastavnicima"
            );
        }

        if (!courseRepository.existsByIdAndTeachers_Id(
                courseId,
                teacher.getId()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Nastavnik ne predaje ovaj predmet"
            );
        }

        return examSessionRepository
                .findByIdAndCourseId(sessionId, courseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ispitni termin ne postoji za ovaj predmet"
                ));
    }

    // PRETVARANJE ENTITETA U DTO
    private TeacherAssignmentResponse toResponse(Assignment assignment) {

        Long groupId = assignment.getExamGroup() == null
                ? null
                : assignment.getExamGroup().getId();

        return new TeacherAssignmentResponse(
                assignment.getId(),
                assignment.getExamSession().getId(),
                groupId,
                assignment.getTitle(),
                assignment.getDescription()
        );
    }
}