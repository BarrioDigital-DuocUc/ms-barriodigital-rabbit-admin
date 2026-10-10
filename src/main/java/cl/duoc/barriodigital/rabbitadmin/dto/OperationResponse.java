package cl.duoc.barriodigital.rabbitadmin.dto;

import java.time.Instant;
// DTO para la respuesta de una operación en RabbitMQ
public record OperationResponse(
        String operation,
        String resource,
        String name,
        String message,
        Instant timestamp
) {
}