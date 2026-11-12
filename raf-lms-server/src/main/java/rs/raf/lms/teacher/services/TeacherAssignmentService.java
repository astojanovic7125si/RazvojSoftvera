package rs.raf.lms.teacher.services;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import rs.raf.lms.exceptions.*;
import rs.raf.lms.model.*;
import rs.raf.lms.repositories.*;
import rs.raf.lms.storage.ProjectStorageService;
import rs.raf.lms.teacher.dto.request.CreateAssignmentRequest;
import rs.raf.lms.teacher.dto.request.UpdateAssignmentRequest;
import rs.raf.lms.teacher.dto.response.TeacherAssignmentResponse;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TeacherAssignmentService {

    private final TeacherAccessService teacherAccessService;
    private final ExamGroupRepository examGroupRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final ProjectStorageService projectStorageService;

    // POSTAVLJANJE ZIP PROJEKTA ZA POSTOJECI ZADATAK
    public TeacherAssignmentResponse uploadProject(
            String username, Long courseId, Long sessionId,
            Long assignmentId, MultipartFile file
    ) {
        Assignment assignment = requireAuthorizedAssignment(
                username, courseId, sessionId, assignmentId
        );

        if (assignment.getStorageKey() != null) {
            throw new ConflictException("Zadatak vec ima ZIP projekat"
            );
        }

        ProjectStorageService.StoredProject stored = projectStorageService.storeZip(file);
        assignment.setOriginalFileName(stored.originalFileName());
        assignment.setStorageKey(stored.storageKey());

        return toResponse(assignmentRepository.save(assignment));
    }

    // PREUZIMANJE ZIP PROJEKTA (SAMO NASTAVNIK TOG PREDMETA)
    @Transactional(readOnly = true)
    public ProjectFile downloadProject(
            String username, Long courseId, Long sessionId, Long assignmentId
    ) {
        Assignment assignment = requireAuthorizedAssignment(
                username, courseId, sessionId, assignmentId
        );

        if (assignment.getStorageKey() == null) {
            throw new ResourceNotFoundException("Zadatak nema postavljen ZIP projekat"
            );
        }

        Resource resource = projectStorageService.loadZip(assignment.getStorageKey());
        return new ProjectFile(resource, assignment.getOriginalFileName());
    }

    public record ProjectFile(Resource resource, String originalFileName) {
    }

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
                    .orElseThrow(() -> new BadRequestException("Ispitna grupa ne pripada ovom terminu"
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
                .orElseThrow(() -> new ResourceNotFoundException("Zadatak ne postoji u ovom terminu"
                ));

        // 3. Proveravamo grupu ako je navedena
        ExamGroup group = null;

        if (request.examGroupId() != null) {
            group = examGroupRepository
                    .findByIdAndExamSessionId(
                            request.examGroupId(),
                            sessionId
                    )
                    .orElseThrow(() -> new BadRequestException("Ispitna grupa ne pripada ovom terminu"
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
                .orElseThrow(() -> new ResourceNotFoundException("Zadatak ne postoji u ovom terminu"
                ));

        // 3. Proveravamo da li postoje studentske predaje
        boolean hasSubmissions =
                submissionRepository.existsByAssignment_Id(assignmentId);

        if (hasSubmissions) {
            throw new ConflictException("Zadatak ne moze biti obrisan jer postoje studentske predaje"
            );
        }

        // 4. Brisemo zadatak
        assignmentRepository.delete(assignment);
    }

    // ZAJEDNICKA PROVERA PRISTUPA
    private Assignment requireAuthorizedAssignment(
            String username, Long courseId, Long sessionId, Long assignmentId
    ) {
        requireAuthorizedSession(username, courseId, sessionId);
        return assignmentRepository.findByIdAndExamSessionId(assignmentId, sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Zadatak ne postoji u ovom terminu"
                ));
    }

    private ExamSession requireAuthorizedSession(
            String username,
            Long courseId,
            Long sessionId
    ) {
        return teacherAccessService.requireAuthorizedSession(username, courseId, sessionId);
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
                assignment.getDescription(),
                assignment.getOriginalFileName()
        );
    }
}
