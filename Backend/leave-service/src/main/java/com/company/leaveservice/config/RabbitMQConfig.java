package com.company.leaveservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ─── Exchange Names ────────────────────────────────────────
    public static final String AUTH_EXCHANGE       = "auth.exchange";
    public static final String LEAVE_EXCHANGE      = "leave.exchange";

    // ─── Queue Names (only queues this service consumes) ──────
    public static final String USER_REGISTERED_QUEUE     = "leave.user.registered.queue";
    public static final String USER_DELETED_QUEUE        = "leave.user.deleted.queue";

    // ─── Routing Keys ──────────────────────────────────────────
    public static final String USER_REGISTERED_KEY     = "user.registered";
    public static final String USER_DELETED_KEY        = "user.deleted";
    public static final String LEAVE_APPLIED_KEY       = "leave.applied";
    public static final String LEAVE_APPROVED_KEY      = "leave.approved";
    public static final String LEAVE_REJECTED_KEY      = "leave.rejected";
    public static final String LEAVE_CANCELLED_KEY     = "leave.cancelled";

    // ─── Exchanges ─────────────────────────────────────────────
    @Bean
    public TopicExchange authExchange() {
        return new TopicExchange(AUTH_EXCHANGE);
    }

    @Bean
    public TopicExchange leaveExchange() {
        return new TopicExchange(LEAVE_EXCHANGE);
    }

    // ─── Queues ────────────────────────────────────────────────
    @Bean public Queue userRegisteredQueue() {
        return QueueBuilder.durable(USER_REGISTERED_QUEUE).build();
    }
    @Bean public Queue userDeletedQueue() {
        return QueueBuilder.durable(USER_DELETED_QUEUE).build();
    }

    // ─── Bindings ──────────────────────────────────────────────
    @Bean public Binding userRegisteredBinding() {
        return BindingBuilder.bind(userRegisteredQueue())
                .to(authExchange()).with(USER_REGISTERED_KEY);
    }
    @Bean public Binding userDeletedBinding() {
        return BindingBuilder.bind(userDeletedQueue())
                .to(authExchange()).with(USER_DELETED_KEY);
    }

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
