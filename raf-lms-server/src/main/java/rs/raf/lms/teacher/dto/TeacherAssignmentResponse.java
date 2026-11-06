package rs.raf.lms.teacher.dto;

public record TeacherAssignmentResponse(

        Long id,
        Long examSessionId,
        Long examGroupId,
        String title,
        String description,
        String projectFileName

) {
}
