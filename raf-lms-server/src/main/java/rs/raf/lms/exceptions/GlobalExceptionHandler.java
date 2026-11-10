package rs.raf.lms.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

// Jedno mesto koje pretvara izuzetke u JSON odgovor {"message": "..."}
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Sve nase greske (404, 403, 409, 400, 401, 500)
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handleApi(ApiException exception) {
        return ResponseEntity
                .status(exception.getStatus())
                .body(Map.of("message", exception.getMessage()));
    }

    // Pogresno korisnicko ime ili lozinka
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(
            BadCredentialsException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Neispravno korisnicko ime ili lozinka"));
    }

    // Greske iz @Valid: vraca poruku za svako polje
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException exception
    ) {
        Map<String, String> errors = new LinkedHashMap<>();

        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }

        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "message", "Podaci nisu ispravni",
                        "errors", errors
                ));
    }

    // Telo zahteva nije ispravan JSON
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadable(
            HttpMessageNotReadableException exception
    ) {
        return ResponseEntity
                .badRequest()
                .body(Map.of("message", "Zahtev nije ispravan JSON"));
    }
}