package cl.duoc.barriodigital.rabbitadmin.exception;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
// Manejador global de excepciones para la aplicación
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException exception) {

        Map<String, List<String>> errors =
                new LinkedHashMap<>();

        for (FieldError error :
                exception.getBindingResult().getFieldErrors()) {

            errors.computeIfAbsent(
                    error.getField(),
                    key -> new ArrayList<>()
            ).add(error.getDefaultMessage());
        }

        Map<String, Object> body =
                baseError(
                        HttpStatus.BAD_REQUEST,
                        "Datos de entrada inválidos"
                );

        body.put("errors", errors);

        return ResponseEntity
                .badRequest()
                .body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    // Maneja las excepciones de violación de restricciones de validación
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException exception) {

        Map<String, Object> body =
                baseError(
                        HttpStatus.BAD_REQUEST,
                        "Parámetro inválido"
                );

        body.put(
                "detail",
                exception.getMessage()
        );

        return ResponseEntity
                .badRequest()
                .body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    // Maneja las excepciones de argumentos ilegales
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException exception) {

        Map<String, Object> body =
                baseError(
                        HttpStatus.BAD_REQUEST,
                        exception.getMessage()
                );

        return ResponseEntity
                .badRequest()
                .body(body);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    // Maneja recursos de RabbitMQ inexistentes
    public ResponseEntity<Map<String, Object>> handleNotFound(
            ResourceNotFoundException exception) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(
                        baseError(
                                HttpStatus.NOT_FOUND,
                                exception.getMessage()
                        )
                );
    }

    @ExceptionHandler(AmqpException.class)
    // Maneja las excepciones relacionadas con RabbitMQ
    public ResponseEntity<Map<String, Object>> handleRabbitMq(
            AmqpException exception) {

        Map<String, Object> body =
                baseError(
                        HttpStatus.BAD_GATEWAY,
                        "RabbitMQ rechazó la operación"
                );

        body.put(
                "detail",
                exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(body);
    }
    // Método auxiliar para construir la respuesta de error base
    private Map<String, Object> baseError(
            HttpStatus status,
            String message) {

        Map<String, Object> body =
                new LinkedHashMap<>();

        body.put(
                "timestamp",
                Instant.now()
        );

        body.put(
                "status",
                status.value()
        );

        body.put(
                "error",
                status.getReasonPhrase()
        );

        body.put(
                "message",
                message
        );

        return body;
    }
}