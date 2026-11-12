package rs.raf.lms.teacher.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.lms.model.Assignment;
import rs.raf.lms.model.ExamAttempt;
import rs.raf.lms.model.ExamRegistration;
import rs.raf.lms.model.SubmissionKind;
import rs.raf.lms.repositories.AssignmentRepository;
import rs.raf.lms.repositories.ExamAttemptRepository;
import rs.raf.lms.repositories.ExamRegistrationRepository;
import rs.raf.lms.repositories.SubmissionRepository;
import rs.raf.lms.teacher.dto.response.MonitoringResponse;
import rs.raf.lms.teacher.dto.response.MonitoringResponse.StudentRow;
import rs.raf.lms.teacher.dto.response.MonitoringStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherMonitoringService {

    private final TeacherAccessService teacherAccessService;
    private final ExamRegistrationRepository examRegistrationRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;

    public MonitoringResponse getMonitoring(String username, Long courseId, Long sessionId) {
        teacherAccessService.requireAuthorizedSession(username, courseId, sessionId);

        // 1. Sve što nam treba, učitano odjednom
        List<ExamRegistration> registrations =
                examRegistrationRepository.findAllWithDetailsBySessionId(sessionId);

        Map<Long, ExamAttempt> attemptByRegistration = new HashMap<>();
        for (ExamAttempt attempt : examAttemptRepository.findByExamRegistration_ExamSession_Id(sessionId)) {
            attemptByRegistration.put(attempt.getExamRegistration().getId(), attempt);
        }

        List<Assignment> assignments = assignmentRepository.findByExamSessionId(sessionId);

        Map<Long, Long> submittedByAttempt = new HashMap<>();
        for (SubmissionRepository.FinalSubmissionCount row :
                submissionRepository.countAssignmentsByAttempt(sessionId, SubmissionKind.FINAL)) {
            submittedByAttempt.put(row.getAttemptId(), row.getAssignmentCount());
        }

        // 2. Za svakog prijavljenog studenta određujemo status
        List<StudentRow> rows = new ArrayList<>();
        List<Long> durationsInSeconds = new ArrayList<>();
        int notStarted = 0;
        int inProgress = 0;
        int submittedStudents = 0;

        for (ExamRegistration registration : registrations) {
            Long groupId = registration.getExamGroup() == null
                    ? null
                    : registration.getExamGroup().getId();

            int total = countApplicable(assignments, groupId);
            ExamAttempt attempt = attemptByRegistration.get(registration.getId());

            MonitoringStatus status;
            int submitted = 0;
            LocalDateTime startedAt = null;
            LocalDateTime lastSubmissionAt = null;
            Long durationMinutes = null;

            if (attempt == null) {
                status = MonitoringStatus.NOT_STARTED;
                notStarted++;
            } else {
                startedAt = attempt.getStartedAt();
                lastSubmissionAt = attempt.getFinishedAt();
                submitted = submittedByAttempt.getOrDefault(attempt.getId(), 0L).intValue();

                if (total > 0 && submitted >= total) {
                    status = MonitoringStatus.SUBMITTED;
                    submittedStudents++;
                    if (lastSubmissionAt != null) {
                        long seconds = Duration.between(startedAt, lastSubmissionAt).toSeconds();
                        durationMinutes = seconds / 60;
                        durationsInSeconds.add(seconds);
                    }
                } else {
                    status = MonitoringStatus.IN_PROGRESS;
                    inProgress++;
                }
            }

            rows.add(new StudentRow(
                    registration.getId(),
                    registration.getStudent().getIndexNumber(),
                    registration.getStudent().getUser().getFirstName(),
                    registration.getStudent().getUser().getLastName(),
                    registration.getExamGroup() == null ? null : registration.getExamGroup().getName(),
                    registration.getClassroom(),
                    status,
                    startedAt,
                    lastSubmissionAt,
                    durationMinutes,
                    submitted,
                    total
            ));
        }

        // 3. Prosečno vreme rada u minutima, jedna decimala, samo za one koji su predali
        Double average = null;
        if (!durationsInSeconds.isEmpty()) {
            double averageSeconds = durationsInSeconds.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0);
            average = Math.round(averageSeconds / 60.0 * 10) / 10.0;
        }

        return new MonitoringResponse(
                registrations.size(), notStarted, inProgress, submittedStudents, average, rows
        );
    }

    // Zadaci koji važu za studenta: oni bez grupe + oni iz njegove grupe
    // Zadatak bez postavljenog ZIP-a se ne računa, jer ga student ne može ni da preuzme ni da preda
    private int countApplicable(List<Assignment> assignments, Long groupId) {
        int count = 0;
        for (Assignment assignment : assignments) {
            if (assignment.getStorageKey() == null) {
                continue;
            }
            if (assignment.getExamGroup() == null
                    || assignment.getExamGroup().getId().equals(groupId)) {
                count++;
            }
        }
        return count;
    }
}
