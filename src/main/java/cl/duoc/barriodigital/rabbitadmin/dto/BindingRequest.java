package cl.duoc.barriodigital.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// DTO para la solicitud de creación de un binding en RabbitMQ
public record BindingRequest(

        @NotBlank(message = "El nombre del exchange es obligatorio")
        @Size(max = 120)
        @Pattern(
                regexp = "^[A-Za-z0-9._-]+$",
                message = "El nombre del exchange contiene caracteres inválidos"
        )
        String exchange,

        @NotBlank(message = "El nombre de la cola es obligatorio")
        @Size(max = 120)
        @Pattern(
                regexp = "^[A-Za-z0-9._-]+$",
                message = "El nombre de la cola contiene caracteres inválidos"
        )
        String queue,

        @NotBlank(message = "La routing key es obligatoria")
        @Size(max = 120)
        @Pattern(
                regexp = "^[A-Za-z0-9._*#-]+$",
                message = "La routing key contiene caracteres inválidos"
        )
        String routingKey
) {
}