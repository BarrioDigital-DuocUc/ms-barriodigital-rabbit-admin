package cl.duoc.barriodigital.rabbitadmin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import cl.duoc.barriodigital.rabbitadmin.dto.BindingRequest;
import cl.duoc.barriodigital.rabbitadmin.dto.ExchangeRequest;
import cl.duoc.barriodigital.rabbitadmin.dto.ExchangeRequest.ExchangeType;
import cl.duoc.barriodigital.rabbitadmin.dto.QueueInfoResponse;
import cl.duoc.barriodigital.rabbitadmin.dto.QueueRequest;
import cl.duoc.barriodigital.rabbitadmin.exception.ResourceNotFoundException;

class RabbitAdminServiceTest {

    private RabbitAdmin rabbitAdmin;
    private RabbitTemplate rabbitTemplate;
    private RabbitAdminService service;

    @BeforeEach
    void setUp() {
        rabbitAdmin = mock(RabbitAdmin.class);
        rabbitTemplate = mock(RabbitTemplate.class);
        when(rabbitAdmin.getRabbitTemplate()).thenReturn(rabbitTemplate);
        service = new RabbitAdminService(rabbitAdmin);
    }

    private void exchangeExists(boolean exists) {
        when(rabbitTemplate.execute(any())).thenReturn(exists);
    }

    private void queueExists(String name) {
        when(rabbitAdmin.getQueueInfo(name)).thenReturn(new QueueInformation(name, 3, 1));
    }

    @Test
    void createQueueDeclaresDurableQueueWithDeadLetter() {
        exchangeExists(true);

        service.createQueue(new QueueRequest("q.demo", true, false, false, "cmd.dead.dlx", "demo.dlq"));

        ArgumentCaptor<Queue> captor = ArgumentCaptor.forClass(Queue.class);
        verify(rabbitAdmin).declareQueue(captor.capture());
        Queue queue = captor.getValue();
        assertThat(queue.getName()).isEqualTo("q.demo");
        assertThat(queue.isDurable()).isTrue();
        assertThat(queue.getArguments())
                .containsEntry("x-dead-letter-exchange", "cmd.dead.dlx")
                .containsEntry("x-dead-letter-routing-key", "demo.dlq");
    }

    @Test
    void createQueueRejectsDurableExclusive() {
        assertThatThrownBy(() -> service.createQueue(new QueueRequest("q.demo", true, true, false, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(rabbitAdmin, never()).declareQueue(any());
    }

    @Test
    void createQueueRejectsIncompleteDeadLetterConfig() {
        assertThatThrownBy(() -> service.createQueue(new QueueRequest("q.demo", true, false, false, "cmd.dead.dlx", null)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(rabbitAdmin, never()).declareQueue(any());
    }

    @Test
    void createQueueRejectsUnknownDeadLetterExchange() {
        exchangeExists(false);

        assertThatThrownBy(() -> service.createQueue(new QueueRequest("q.demo", true, false, false, "no.existe", "demo.dlq")))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(rabbitAdmin, never()).declareQueue(any());
    }

    @Test
    void createQueueRejectsReservedPrefix() {
        assertThatThrownBy(() -> service.createQueue(new QueueRequest("amq.demo", true, false, false, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amq.");
    }

    @Test
    void getQueueReturnsCounters() {
        queueExists("q.cmd.email.dlq");

        QueueInfoResponse info = service.getQueue("q.cmd.email.dlq");

        assertThat(info.messageCount()).isEqualTo(3);
        assertThat(info.consumerCount()).isEqualTo(1);
    }

    @Test
    void deleteMissingQueueThrowsNotFound() {
        assertThatThrownBy(() -> service.deleteQueue("q.no.existe"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(rabbitAdmin, never()).deleteQueue(any());
    }

    @Test
    void deleteExistingQueue() {
        queueExists("q.demo");

        service.deleteQueue("q.demo");

        verify(rabbitAdmin).deleteQueue("q.demo");
    }

    @Test
    void createExchangeDeclaresTopic() {
        service.createExchange(new ExchangeRequest("demo.topic", ExchangeType.TOPIC, true, false));

        ArgumentCaptor<Exchange> captor = ArgumentCaptor.forClass(Exchange.class);
        verify(rabbitAdmin).declareExchange(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("topic");
    }

    @Test
    void deleteMissingExchangeThrowsNotFound() {
        exchangeExists(false);

        assertThatThrownBy(() -> service.deleteExchange("no.existe"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(rabbitAdmin, never()).deleteExchange(any());
    }

    @Test
    void createBindingRequiresExistingExchangeAndQueue() {
        exchangeExists(true);
        queueExists("q.demo");

        service.createBinding(new BindingRequest("demo.direct", "q.demo", "demo.key"));

        ArgumentCaptor<Binding> captor = ArgumentCaptor.forClass(Binding.class);
        verify(rabbitAdmin).declareBinding(captor.capture());
        assertThat(captor.getValue().getRoutingKey()).isEqualTo("demo.key");
    }

    @Test
    void createBindingToMissingQueueThrowsNotFound() {
        exchangeExists(true);

        assertThatThrownBy(() -> service.createBinding(new BindingRequest("demo.direct", "q.no.existe", "demo.key")))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(rabbitAdmin, never()).declareBinding(any());
    }
}
