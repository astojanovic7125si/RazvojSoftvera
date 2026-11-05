package rs.raf.lms.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "Korisnicko ime je obavezno")
        String username,

        @NotBlank(message = "Lozinka je obavezna")
        String password

) {
}