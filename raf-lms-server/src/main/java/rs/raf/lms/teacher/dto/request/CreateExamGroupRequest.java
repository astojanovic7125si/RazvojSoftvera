package rs.raf.lms.teacher.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateExamGroupRequest(
        @NotBlank @Size(max = 50) String name
) {
}