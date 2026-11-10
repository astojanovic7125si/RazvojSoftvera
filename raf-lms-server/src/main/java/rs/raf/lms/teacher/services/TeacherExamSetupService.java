package rs.raf.lms.teacher.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.lms.exceptions.BadRequestException;
import rs.raf.lms.exceptions.ConflictException;
import rs.raf.lms.model.Course;
import rs.raf.lms.model.ExamGroup;
import rs.raf.lms.model.ExamPeriod;
import rs.raf.lms.model.ExamSession;
import rs.raf.lms.repositories.ExamGroupRepository;
import rs.raf.lms.repositories.ExamPeriodRepository;
import rs.raf.lms.repositories.ExamSessionRepository;
import rs.raf.lms.teacher.dto.request.CreateExamGroupRequest;
import rs.raf.lms.teacher.dto.request.CreateExamPeriodRequest;
import rs.raf.lms.teacher.dto.request.CreateExamSessionRequest;
import rs.raf.lms.teacher.dto.response.ExamGroupResponse;
import rs.raf.lms.teacher.dto.response.ExamPeriodResponse;
import rs.raf.lms.teacher.dto.response.TeacherExamSessionResponse;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TeacherExamSetupService {

    private final TeacherAccessService teacherAccessService;
    private final ExamPeriodRepository examPeriodRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamGroupRepository examGroupRepository;

    // ---------- ISPITNI ROKOVI ----------
    // Rok nije vezan za predmet (zajednički je za ceo fakultet), pa ovde nema provere predmeta
    // Da je korisnik nastavnik garantuje SecurityConfig (/api/teacher/**)

    @Transactional(readOnly = true)
    public List<ExamPeriodResponse> getPeriods() {
        return examPeriodRepository.findAll().stream()
                .sorted(Comparator.comparing(ExamPeriod::getStartDate))
                .map(this::toResponse)
                .toList();
    }

    public ExamPeriodResponse createPeriod(CreateExamPeriodRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new BadRequestException("Kraj roka ne može biti pre početka roka");
        }

        ExamPeriod period = examPeriodRepository.save(
                new ExamPeriod(request.name().trim(), request.startDate(), request.endDate())
        );
        return toResponse(period);
    }

    // ---------- TERMINI ----------

    public TeacherExamSessionResponse createSession(
            String username, Long courseId, CreateExamSessionRequest request
    ) {
        // termin se pravi samo za predmet koji ovaj nastavnik predaje
        Course course = teacherAccessService.requireAuthorizedCourse(username, courseId);

        ExamPeriod period = examPeriodRepository.findById(request.examPeriodId())
                .orElseThrow(() -> new BadRequestException("Ispitni rok sa ovim ID ne postoji"));

        if (!request.endTime().isAfter(request.startTime())) {
            throw new BadRequestException("Kraj termina mora biti posle početka termina");
        }

        ExamSession session = examSessionRepository.save(new ExamSession(
                course, period, request.startTime(), request.endTime(), request.maxPoints()
        ));

        return new TeacherExamSessionResponse(
                session.getId(),
                period.getId(),
                period.getName(),
                session.getStartTime(),
                session.getEndTime(),
                session.getMaxPoints()
        );
    }

    // ---------- GRUPE ----------

    @Transactional(readOnly = true)
    public List<ExamGroupResponse> getGroups(String username, Long courseId, Long sessionId) {
        teacherAccessService.requireAuthorizedSession(username, courseId, sessionId);

        return examGroupRepository.findByExamSessionId(sessionId).stream()
                .map(g -> new ExamGroupResponse(g.getId(), g.getName()))
                .toList();
    }

    public ExamGroupResponse createGroup(
            String username, Long courseId, Long sessionId, CreateExamGroupRequest request
    ) {
        ExamSession session = teacherAccessService
                .requireAuthorizedSession(username, courseId, sessionId);

        String name = request.name().trim();

        // u jednom terminu ne mogu da postoje dve grupe sa istim imenom
        if (examGroupRepository.findByExamSessionIdAndName(sessionId, name).isPresent()) {
            throw new ConflictException("Grupa '" + name + "' već postoji u ovom terminu");
        }

        ExamGroup group = examGroupRepository.save(new ExamGroup(session, name));
        return new ExamGroupResponse(group.getId(), group.getName());
    }

    private ExamPeriodResponse toResponse(ExamPeriod p) {
        return new ExamPeriodResponse(p.getId(), p.getName(), p.getStartDate(), p.getEndDate());
    }
}