package rs.raf.lms.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void povredaUniqueOgranicenja_vracaConflictUmestoGreske500() {
        ResponseEntity<Map<String, String>> odgovor =
                handler.handleIntegrity(new DataIntegrityViolationException("duplicate key"));

        assertThat(odgovor.getStatusCode().value()).isEqualTo(409);
        assertThat(odgovor.getBody()).containsKey("message");
    }
}