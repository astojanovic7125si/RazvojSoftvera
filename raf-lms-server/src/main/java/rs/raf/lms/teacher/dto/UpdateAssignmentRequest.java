package rs.raf.lms.teacher.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAssignmentRequest(

        @NotBlank(message = "Naslov zadatka je obavezan")
        @Size(max = 150, message = "Naslov moze imati najvise 150 karaktera")
        String title,

        String description,

        Long examGroupId

) {
}