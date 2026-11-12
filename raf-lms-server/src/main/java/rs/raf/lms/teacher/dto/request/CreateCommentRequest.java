package rs.raf.lms.teacher.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank @Size(max = 500) String filePath,
        @NotNull @Min(1) Integer lineNumber,
        @NotBlank @Size(max = 5000) String content
) {
}
