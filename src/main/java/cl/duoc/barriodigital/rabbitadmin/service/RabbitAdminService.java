package cl.duoc.barriodigital.rabbitadmin.service;

import java.io.IOException;
import java.util.Map;

import org.springframework.amqp.core.AbstractExchange;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import cl.duoc.barriodigital.rabbitadmin.dto.BindingRequest;
import cl.duoc.barriodigital.rabbitadmin.dto.ExchangeRequest;
import cl.duoc.barriodigital.rabbitadmin.dto.QueueInfoResponse;
import cl.duoc.barriodigital.rabbitadmin.dto.QueueRequest;
import cl.duoc.barriodigital.rabbitadmin.exception.ResourceNotFoundException;

@Service
// Servicio para administrar colas, exchanges y bindings en RabbitMQ
public class RabbitAdminService {

    // RabbitMQ reserva el prefijo amq. y rechaza declararlo con 403 ACCESS_REFUSED
    private static final String RESERVED_PREFIX = "amq.";

    private final RabbitAdmin rabbitAdmin;

    public RabbitAdminService(
            RabbitAdmin rabbitAdmin) {

        this.rabbitAdmin = rabbitAdmin;
    }

    // =========================================================
    // QUEUES
    // =========================================================

    public void createQueue(
            QueueRequest request) {

        assertNotReserved(request.name());

        boolean durable =
                request.durable() == null
                        || request.durable();

        boolean exclusive =
                Boolean.TRUE.equals(
                        request.exclusive()
                );

        boolean autoDelete =
                Boolean.TRUE.equals(
                        request.autoDelete()
                );

        validateQueueConfiguration(
                durable,
                exclusive,
                request.deadLetterExchange(),
                request.deadLetterRoutingKey()
        );

        QueueBuilder builder =
                durable
                        ? QueueBuilder.durable(request.name())
                        : QueueBuilder.nonDurable(request.name());

        if (exclusive) {
            builder.exclusive();
        }

        if (autoDelete) {
            builder.autoDelete();
        }

        if (StringUtils.hasText(
                request.deadLetterExchange())) {

            requireExchange(request.deadLetterExchange());

            builder.deadLetterExchange(
                    request.deadLetterExchange()
            );

            builder.deadLetterRoutingKey(
                    request.deadLetterRoutingKey()
            );
        }

        Queue queue =
                builder.build();

        rabbitAdmin.declareQueue(queue);
    }

    public QueueInfoResponse getQueue(
            String name) {

        QueueInformation info =
                requireQueue(name);

        return new QueueInfoResponse(
                info.getName(),
                info.getMessageCount(),
                info.getConsumerCount()
        );
    }

    public void deleteQueue(
            String name) {

        assertNotReserved(name);
        requireQueue(name);

        rabbitAdmin.deleteQueue(name);
    }


    // =========================================================
    // EXCHANGES
    // =========================================================

    public void createExchange(
            ExchangeRequest request) {

        assertNotReserved(request.name());

        boolean durable =
                request.durable() == null
                        || request.durable();

        boolean autoDelete =
                Boolean.TRUE.equals(
                        request.autoDelete()
                );

        AbstractExchange exchange =
                switch (request.type()) {

                    case DIRECT ->
                            new DirectExchange(
                                    request.name(),
                                    durable,
                                    autoDelete
                            );

                    case TOPIC ->
                            new TopicExchange(
                                    request.name(),
                                    durable,
                                    autoDelete
                            );
                };

        rabbitAdmin.declareExchange(exchange);
    }

    public void deleteExchange(
            String name) {

        assertNotReserved(name);
        requireExchange(name);

        rabbitAdmin.deleteExchange(name);
    }


    // =========================================================
    // BINDINGS
    // =========================================================

    public void createBinding(
            BindingRequest request) {

        requireExchange(request.exchange());
        requireQueue(request.queue());

        rabbitAdmin.declareBinding(
                buildBinding(request)
        );
    }

    public void deleteBinding(
            BindingRequest request) {

        requireExchange(request.exchange());
        requireQueue(request.queue());

        rabbitAdmin.removeBinding(
                buildBinding(request)
        );
    }


    // =========================================================
    // HELPERS
    // =========================================================

    private Binding buildBinding(
            BindingRequest request) {

        return new Binding(
                request.queue(),
                Binding.DestinationType.QUEUE,
                request.exchange(),
                request.routingKey(),
                Map.of()
        );
    }

    private QueueInformation requireQueue(
            String name) {

        QueueInformation info =
                rabbitAdmin.getQueueInfo(name);

        if (info == null) {

            throw new ResourceNotFoundException(
                    "La cola no existe: " + name
            );
        }

        return info;
    }

    private void requireExchange(
            String name) {

        if (!exchangeExists(name)) {

            throw new ResourceNotFoundException(
                    "El exchange no existe: " + name
            );
        }
    }

    private boolean exchangeExists(
            String name) {

        // RabbitAdmin no expone consulta de exchanges; la declaración pasiva
        // falla con 404 y cierra el canal si no existe
        Boolean exists =
                rabbitAdmin.getRabbitTemplate().execute(channel -> {

                    try {
                        channel.exchangeDeclarePassive(name);
                        return true;
                    } catch (IOException notFound) {
                        return false;
                    }
                });

        return Boolean.TRUE.equals(exists);
    }

    private void assertNotReserved(
            String name) {

        if (name != null
                && name.startsWith(RESERVED_PREFIX)) {

            throw new IllegalArgumentException(
                    "El prefijo 'amq.' está reservado por RabbitMQ: "
                            + name
            );
        }
    }

    private void validateQueueConfiguration(
            boolean durable,
            boolean exclusive,
            String deadLetterExchange,
            String deadLetterRoutingKey) {

        if (durable && exclusive) {

            throw new IllegalArgumentException(
                    "Una cola exclusive no puede configurarse como durable"
            );
        }

        boolean hasDlx =
                StringUtils.hasText(
                        deadLetterExchange
                );

        boolean hasDlrk =
                StringUtils.hasText(
                        deadLetterRoutingKey
                );

        if (hasDlx != hasDlrk) {

            throw new IllegalArgumentException(
                    "deadLetterExchange y deadLetterRoutingKey deben configurarse juntos"
            );
        }
    }
}
