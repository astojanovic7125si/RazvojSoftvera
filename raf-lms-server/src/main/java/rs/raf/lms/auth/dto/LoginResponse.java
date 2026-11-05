package rs.raf.lms.auth.dto;

public record LoginResponse(

        String accessToken,
        String tokenType,
        String role

) {
}