package cl.duoc.barriodigital.rabbitadmin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import cl.duoc.barriodigital.rabbitadmin.dto.QueueInfoResponse;
import cl.duoc.barriodigital.rabbitadmin.exception.ResourceNotFoundException;
import cl.duoc.barriodigital.rabbitadmin.service.RabbitAdminService;

@WebMvcTest(RabbitAdminController.class)
class RabbitAdminControllerTest {

    private static final String BASE = "/api/v1/rabbit";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RabbitAdminService service;

    @Test
    void createQueueReturns201() throws Exception {
        mockMvc.perform(post(BASE + "/queues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"q.demo\",\"durable\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resource").value("QUEUE"))
                .andExpect(jsonPath("$.name").value("q.demo"));
    }

    @Test
    void createQueueWithBlankNameReturns400() throws Exception {
        mockMvc.perform(post(BASE + "/queues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());

        verify(service, never()).createQueue(any());
    }

    @Test
    void createExchangeWithoutTypeReturns400() throws Exception {
        mockMvc.perform(post(BASE + "/exchanges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"demo.direct\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.type").exists());
    }

    @Test
    void createBindingWithInvalidRoutingKeyReturns400() throws Exception {
        mockMvc.perform(post(BASE + "/bindings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exchange\":\"demo.direct\",\"queue\":\"q.demo\",\"routingKey\":\"clave con espacios\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.routingKey").exists());
    }

    @Test
    void getQueueReturnsCounters() throws Exception {
        when(service.getQueue("q.cmd.email.dlq")).thenReturn(new QueueInfoResponse("q.cmd.email.dlq", 2, 0));

        mockMvc.perform(get(BASE + "/queues/q.cmd.email.dlq"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageCount").value(2));
    }

    @Test
    void deleteMissingQueueReturns404() throws Exception {
        doThrow(new ResourceNotFoundException("La cola no existe: q.no.existe"))
                .when(service).deleteQueue("q.no.existe");

        mockMvc.perform(delete(BASE + "/queues/q.no.existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("La cola no existe: q.no.existe"));
    }

    @Test
    void reservedPrefixReturns400() throws Exception {
        doThrow(new IllegalArgumentException("El prefijo 'amq.' está reservado por RabbitMQ: amq.demo"))
                .when(service).createQueue(any());

        mockMvc.perform(post(BASE + "/queues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"amq.demo\"}"))
                .andExpect(status().isBadRequest());
    }
}
