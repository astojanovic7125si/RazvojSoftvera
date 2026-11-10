package rs.raf.lms.teacher.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ImportRegistrationsRequest(
        @NotEmpty @Valid List<RegistrationEntryRequest> students
) {
}