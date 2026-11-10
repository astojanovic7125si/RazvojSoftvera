package rs.raf.lms.teacher.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationEntryRequest(
        @NotBlank @Size(max = 30) String indexNumber,
        @Size(max = 50) String groupName,
        @Size(max = 50) String classroom
) {
}