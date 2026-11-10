package rs.raf.lms.teacher.dto.response;

import java.util.List;

public record ImportRegistrationsResponse(
        int created,
        int updated,
        List<Rejected> rejected
) {
    public record Rejected(String indexNumber, String reason) {
    }
}