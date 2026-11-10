package rs.raf.lms.auth.dto.response;

public record LoginResponse(

        String accessToken,
        String tokenType,
        String role

) {
}