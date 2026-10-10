package cl.duoc.barriodigital.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
// DTO para la solicitud de creación de un exchange en RabbitMQ
public record ExchangeRequest(

        @NotBlank(message = "El nombre del exchange es obligatorio")
        @Size(max = 120)
        @Pattern(
                regexp = "^[A-Za-z0-9._-]+$",
                message = "El nombre del exchange contiene caracteres inválidos"
        )
        String name,

        @NotNull(message = "El tipo de exchange es obligatorio")
        ExchangeType type,

        Boolean durable,

        Boolean autoDelete
) {

    public enum ExchangeType {
        DIRECT,
        TOPIC
    }
}