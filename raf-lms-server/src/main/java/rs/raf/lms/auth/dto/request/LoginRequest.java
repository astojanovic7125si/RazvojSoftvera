package rs.raf.lms.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "Korisnicko ime je obavezno")
        String username,

        @NotBlank(message = "Lozinka je obavezna")
        String password

) {
}