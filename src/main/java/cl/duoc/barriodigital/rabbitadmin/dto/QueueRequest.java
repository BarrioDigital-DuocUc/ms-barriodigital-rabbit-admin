package cl.duoc.barriodigital.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
// DTO para la solicitud de creación de una cola en RabbitMQ
public record QueueRequest(

        @NotBlank(message = "El nombre de la cola es obligatorio")
        @Size(max = 120, message = "El nombre de la cola no puede superar 120 caracteres")
        @Pattern(
                regexp = "^[A-Za-z0-9._-]+$",
                message = "El nombre de la cola contiene caracteres inválidos"
        )
        String name,

        Boolean durable,

        Boolean exclusive,

        Boolean autoDelete,

        @Size(max = 120)
        String deadLetterExchange,

        @Size(max = 120)
        String deadLetterRoutingKey
) {
}