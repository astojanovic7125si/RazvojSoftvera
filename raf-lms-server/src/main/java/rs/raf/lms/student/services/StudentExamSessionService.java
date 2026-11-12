package rs.raf.lms.student.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.lms.model.ExamRegistration;
import rs.raf.lms.model.ExamSession;
import rs.raf.lms.repositories.ExamRegistrationRepository;
import rs.raf.lms.student.dto.response.StudentExamSessionResponse;
import rs.raf.lms.student.dto.response.StudentExamSessionResponse.Status;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentExamSessionService {

    private final ExamRegistrationRepository examRegistrationRepository;

    public List<StudentExamSessionResponse> getMySessions(String username) {
        LocalDateTime now = LocalDateTime.now();

        return examRegistrationRepository
                .findAllForStudent(username)
                .stream()
                .map(registration -> toResponse(registration, now))
                .toList();
    }

    private StudentExamSessionResponse toResponse(
            ExamRegistration registration,
            LocalDateTime now
    ) {
        ExamSession session = registration.getExamSession();

        // isto pravilo kao kod preuzimanja: početak uključen, kraj isključen
        Status status;
        if (now.isBefore(session.getStartTime())) {
            status = Status.UPCOMING;
        } else if (now.isBefore(session.getEndTime())) {
            status = Status.ACTIVE;
        } else {
            status = Status.FINISHED;
        }

        return new StudentExamSessionResponse(
                session.getId(),
                session.getCourse().getId(),
                session.getCourse().getCode(),
                session.getCourse().getName(),
                session.getExamPeriod().getName(),
                session.getStartTime(),
                session.getEndTime(),
                registration.getExamGroup() == null
                        ? null
                        : registration.getExamGroup().getName(),
                registration.getClassroom(),
                status
        );
    }
}