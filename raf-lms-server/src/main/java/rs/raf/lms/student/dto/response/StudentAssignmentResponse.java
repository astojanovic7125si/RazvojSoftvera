package rs.raf.lms.student.dto.response;
//klasa koja nosi podatke->record
public record StudentAssignmentResponse(
        Long id,
        String title,
        String description,
        Long examGroupId,
        String projectFileName
) {
}