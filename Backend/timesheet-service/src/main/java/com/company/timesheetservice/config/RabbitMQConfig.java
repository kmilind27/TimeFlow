package com.company.timesheetservice.config;

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
    public static final String TIMESHEET_EXCHANGE  = "timesheet.exchange";

    // ─── Queue Names (only queues this service consumes) ──────
    public static final String USER_DELETED_QUEUE        = "timesheet.user.deleted.queue";

    // ─── Routing Keys ──────────────────────────────────────────
    public static final String USER_DELETED_KEY        = "user.deleted";
    public static final String TIMESHEET_SUBMITTED_KEY = "timesheet.submitted";
    public static final String TIMESHEET_APPROVED_KEY  = "timesheet.approved";
    public static final String TIMESHEET_REJECTED_KEY  = "timesheet.rejected";

    // ─── Exchanges ─────────────────────────────────────────────
    @Bean
    public TopicExchange authExchange() {
        return new TopicExchange(AUTH_EXCHANGE);
    }

    @Bean
    public TopicExchange timesheetExchange() {
        return new TopicExchange(TIMESHEET_EXCHANGE);
    }

    // ─── Queues ────────────────────────────────────────────────
    @Bean public Queue userDeletedQueue() {
        return QueueBuilder.durable(USER_DELETED_QUEUE).build();
    }

    // ─── Bindings ──────────────────────────────────────────────
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
