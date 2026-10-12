package cl.duoc.barriodigital.rabbitadmin.exception;

// Se lanza cuando la cola o el exchange solicitado no existe en RabbitMQ
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(
            String message) {

        super(message);
    }
}
