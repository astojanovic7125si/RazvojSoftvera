package rs.raf.lms.teacher.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.lms.repositories.ExamSessionRepository;
import rs.raf.lms.teacher.dto.response.TeacherExamSessionResponse;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherExamSessionService {

    private final TeacherAccessService teacherAccessService;
    private final ExamSessionRepository examSessionRepository;

    public List<TeacherExamSessionResponse> getExamSessions(
            String username,
            Long courseId
    ) {
        teacherAccessService.requireAuthorizedCourse(username, courseId);

        return examSessionRepository.findByCourseId(courseId)
                .stream()
                .map(session -> new TeacherExamSessionResponse(
                        session.getId(),
                        session.getExamPeriod().getId(),
                        session.getExamPeriod().getName(),
                        session.getStartTime(),
                        session.getEndTime(),
                        session.getMaxPoints()
                ))
                .toList();
    }
}