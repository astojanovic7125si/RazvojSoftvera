package rs.raf.lms.student.dto;

public record StudentAssignmentResponse(
        Long id,
        String title,
        String description,
        Long examGroupId,
        String projectFileName
) {
}