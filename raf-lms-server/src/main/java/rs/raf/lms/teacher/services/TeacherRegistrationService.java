package rs.raf.lms.teacher.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.lms.model.ExamGroup;
import rs.raf.lms.model.ExamRegistration;
import rs.raf.lms.model.ExamSession;
import rs.raf.lms.model.Student;
import rs.raf.lms.repositories.ExamGroupRepository;
import rs.raf.lms.repositories.ExamRegistrationRepository;
import rs.raf.lms.repositories.StudentRepository;
import rs.raf.lms.teacher.dto.request.ImportRegistrationsRequest;
import rs.raf.lms.teacher.dto.request.RegistrationEntryRequest;
import rs.raf.lms.teacher.dto.response.ImportRegistrationsResponse;
import rs.raf.lms.teacher.dto.response.ImportRegistrationsResponse.Rejected;
import rs.raf.lms.teacher.dto.response.RegistrationResponse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class TeacherRegistrationService {

    private final TeacherAccessService teacherAccessService;
    private final StudentRepository studentRepository;
    private final ExamGroupRepository examGroupRepository;
    private final ExamRegistrationRepository examRegistrationRepository;

    // UNOS SPISKA: za svaki red pravi novu prijavu ili ažurira postojeću.
    // Neispravni redovi se preskaču i vraćaju u listi "rejected".
    public ImportRegistrationsResponse importRegistrations(
            String username, Long courseId, Long sessionId,
            ImportRegistrationsRequest request
    ) {
        ExamSession session = teacherAccessService
                .requireAuthorizedSession(username, courseId, sessionId);

        int created = 0;
        int updated = 0;
        List<Rejected> rejected = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (RegistrationEntryRequest entry : request.students()) {
            String index = entry.indexNumber().trim();

            if (!seen.add(index)) {
                rejected.add(new Rejected(index, "Indeks se u spisku pojavljuje više puta"));
                continue;
            }

            Student student = studentRepository.findByIndexNumber(index).orElse(null);
            if (student == null) {
                rejected.add(new Rejected(index, "Student sa ovim indeksom ne postoji"));
                continue;
            }

            ExamGroup group = null;
            String groupName = blankToNull(entry.groupName());
            if (groupName != null) {
                group = examGroupRepository
                        .findByExamSessionIdAndName(sessionId, groupName)
                        .orElse(null);
                if (group == null) {
                    rejected.add(new Rejected(index, "Grupa '" + groupName + "' ne postoji u ovom terminu"));
                    continue;
                }
            }

            ExamRegistration registration = examRegistrationRepository
                    .findByStudentIdAndExamSessionId(student.getId(), sessionId)
                    .orElse(null);

            if (registration == null) {
                registration = new ExamRegistration(student, session);
                created++;
            } else {
                updated++;
            }

            registration.setExamGroup(group);
            registration.setClassroom(blankToNull(entry.classroom()));
            examRegistrationRepository.save(registration);
        }

        return new ImportRegistrationsResponse(created, updated, rejected);
    }

    // SPISAK PRIJAVLJENIH ZA TERMIN
    @Transactional(readOnly = true)
    public List<RegistrationResponse> getRegistrations(
            String username, Long courseId, Long sessionId
    ) {
        teacherAccessService.requireAuthorizedSession(username, courseId, sessionId);

        return examRegistrationRepository.findAllWithDetailsBySessionId(sessionId)
                .stream()
                .map(r -> new RegistrationResponse(
                        r.getId(),
                        r.getStudent().getIndexNumber(),
                        r.getStudent().getUser().getFirstName(),
                        r.getStudent().getUser().getLastName(),
                        r.getExamGroup() == null ? null : r.getExamGroup().getName(),
                        r.getClassroom()
                ))
                .toList();
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}