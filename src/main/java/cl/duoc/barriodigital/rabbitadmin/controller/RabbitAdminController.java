package cl.duoc.barriodigital.rabbitadmin.controller;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.barriodigital.rabbitadmin.dto.BindingRequest;
import cl.duoc.barriodigital.rabbitadmin.dto.ExchangeRequest;
import cl.duoc.barriodigital.rabbitadmin.dto.OperationResponse;
import cl.duoc.barriodigital.rabbitadmin.dto.QueueRequest;
import cl.duoc.barriodigital.rabbitadmin.service.RabbitAdminService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@RestController
@RequestMapping("/api/v1/rabbit")
@Validated
// Controlador REST para administrar colas, exchanges y bindings en RabbitMQ
public class RabbitAdminController {

    private final RabbitAdminService rabbitAdminService;

    public RabbitAdminController(
            RabbitAdminService rabbitAdminService) {

        this.rabbitAdminService =
                rabbitAdminService;
    }


    // =========================================================
    // QUEUES
    // =========================================================

    @PostMapping("/queues")
    public ResponseEntity<OperationResponse> createQueue(
            @Valid @RequestBody QueueRequest request) {

        rabbitAdminService.createQueue(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        new OperationResponse(
                                "CREATE",
                                "QUEUE",
                                request.name(),
                                "Cola creada correctamente",
                                Instant.now()
                        )
                );
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<OperationResponse> deleteQueue(

            @PathVariable
            @NotBlank
            @Pattern(regexp = "^[A-Za-z0-9._-]+$")
            String name) {

        boolean deleted =
                rabbitAdminService.deleteQueue(name);

        if (!deleted) {

            throw new IllegalArgumentException(
                    "La cola no existe o no pudo ser eliminada: "
                            + name
            );
        }

        return ResponseEntity.ok(
                new OperationResponse(
                        "DELETE",
                        "QUEUE",
                        name,
                        "Cola eliminada correctamente",
                        Instant.now()
                )
        );
    }


    // =========================================================
    // EXCHANGES
    // =========================================================

    @PostMapping("/exchanges")
    public ResponseEntity<OperationResponse> createExchange(
            @Valid @RequestBody ExchangeRequest request) {

        rabbitAdminService.createExchange(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        new OperationResponse(
                                "CREATE",
                                "EXCHANGE",
                                request.name(),
                                "Exchange creado correctamente",
                                Instant.now()
                        )
                );
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<OperationResponse> deleteExchange(

            @PathVariable
            @NotBlank
            @Pattern(regexp = "^[A-Za-z0-9._-]+$")
            String name) {

        boolean deleted =
                rabbitAdminService.deleteExchange(name);

        if (!deleted) {

            throw new IllegalArgumentException(
                    "El exchange no existe o no pudo ser eliminado: "
                            + name
            );
        }

        return ResponseEntity.ok(
                new OperationResponse(
                        "DELETE",
                        "EXCHANGE",
                        name,
                        "Exchange eliminado correctamente",
                        Instant.now()
                )
        );
    }


    // =========================================================
    // BINDINGS
    // =========================================================

    @PostMapping("/bindings")
    public ResponseEntity<OperationResponse> createBinding(
            @Valid @RequestBody BindingRequest request) {

        rabbitAdminService.createBinding(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        new OperationResponse(
                                "CREATE",
                                "BINDING",
                                request.exchange()
                                        + " -> "
                                        + request.queue(),
                                "Binding creado correctamente",
                                Instant.now()
                        )
                );
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<OperationResponse> deleteBinding(
            @Valid @RequestBody BindingRequest request) {

        rabbitAdminService.deleteBinding(request);

        return ResponseEntity.ok(
                new OperationResponse(
                        "DELETE",
                        "BINDING",
                        request.exchange()
                                + " -> "
                                + request.queue(),
                        "Binding eliminado correctamente",
                        Instant.now()
                )
        );
    }
}