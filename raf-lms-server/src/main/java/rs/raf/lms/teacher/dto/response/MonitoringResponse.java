package rs.raf.lms.teacher.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record MonitoringResponse(
        int registered,
        int notStarted,
        int inProgress,
        int submitted,
        Double averageDurationMinutes,   // null ako još niko nije predao
        List<StudentRow> students
) {
    public record StudentRow(
            Long registrationId,
            String indexNumber,
            String firstName,
            String lastName,
            String groupName,
            String classroom,
            MonitoringStatus status,
            LocalDateTime startedAt,         //null ako nije počeo
            LocalDateTime lastSubmissionAt,  //vreme poslednje finalne predaje
            Long durationMinutes,            //samo za SUBMITTED
            int assignmentsSubmitted,        //za koliko zadataka ima FINAL
            int assignmentsTotal             //koliko zadataka mu pripada
    ) {
    }
}
