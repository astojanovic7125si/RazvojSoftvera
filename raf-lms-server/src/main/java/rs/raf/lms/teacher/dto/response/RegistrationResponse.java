package rs.raf.lms.teacher.dto.response;

public record RegistrationResponse(
        Long id,
        String indexNumber,
        String firstName,
        String lastName,
        String groupName,
        String classroom
) {
}