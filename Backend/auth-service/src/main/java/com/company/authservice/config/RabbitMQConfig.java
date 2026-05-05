package com.company.authservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ─── Exchange Name ─────────────────────────────────────────
    public static final String AUTH_EXCHANGE       = "auth.exchange";

    // ─── Routing Keys ──────────────────────────────────────────
    public static final String USER_REGISTERED_KEY     = "user.registered";
    public static final String USER_ROLE_CHANGED_KEY   = "user.role.changed";
    public static final String USER_DELETED_KEY        = "user.deleted";
    public static final String USER_OTP_REQUESTED_KEY  = "user.otp.requested";

    // ─── Exchange ──────────────────────────────────────────────
    @Bean
    public TopicExchange authExchange() {
        return new TopicExchange(AUTH_EXCHANGE);
    }

    // auth-service ONLY publishes events — no queues or bindings needed.

    // ─── JSON Message Converter ────────────────────────────────
    @Bean
    public JacksonJsonMessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    // ─── RabbitTemplate ────────────────────────────────────────
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
