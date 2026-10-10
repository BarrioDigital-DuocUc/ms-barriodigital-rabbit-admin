package cl.duoc.barriodigital.rabbitadmin.service;

import java.util.Map;

import org.springframework.amqp.core.AbstractExchange;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import cl.duoc.barriodigital.rabbitadmin.dto.BindingRequest;
import cl.duoc.barriodigital.rabbitadmin.dto.ExchangeRequest;
import cl.duoc.barriodigital.rabbitadmin.dto.QueueRequest;

@Service
// Servicio para administrar colas, exchanges y bindings en RabbitMQ
public class RabbitAdminService {

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

    public boolean deleteQueue(
            String name) {

        return rabbitAdmin.deleteQueue(name);
    }


    // =========================================================
    // EXCHANGES
    // =========================================================

    public void createExchange(
            ExchangeRequest request) {

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

    public boolean deleteExchange(
            String name) {

        return rabbitAdmin.deleteExchange(name);
    }


    // =========================================================
    // BINDINGS
    // =========================================================

    public void createBinding(
            BindingRequest request) {

        Binding binding =
                buildBinding(request);

        rabbitAdmin.declareBinding(binding);
    }

    public void deleteBinding(
            BindingRequest request) {

        Binding binding =
                buildBinding(request);

        rabbitAdmin.removeBinding(binding);
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