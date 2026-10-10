package cl.duoc.barriodigital.rabbitadmin.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
// Clase de configuración para RabbitAdmin
public class RabbitAdminConfig {

    @Bean
    public RabbitAdmin rabbitAdmin(
            ConnectionFactory connectionFactory) {

        return new RabbitAdmin(connectionFactory);
    }
}