package cl.duoc.barriodigital.rabbitadmin.dto;

// DTO con el estado actual de una cola en RabbitMQ
public record QueueInfoResponse(
        String name,
        long messageCount,
        int consumerCount
) {
}
