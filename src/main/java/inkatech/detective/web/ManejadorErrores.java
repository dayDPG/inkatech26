package inkatech.detective.web;

import java.util.Map;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> argumentoInvalido(IllegalArgumentException error) {
        String mensaje = Objects.requireNonNullElse(error.getMessage(), "Solicitud invalida");
        return ResponseEntity.badRequest().body(Map.of("mensaje", mensaje));
    }
}

