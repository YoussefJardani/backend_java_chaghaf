package ma.chaghaf.notification.event;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration RabbitMQ pour le bus de notifications.
 *
 * Activée via :
 *   chaghaf.rabbitmq.enabled=true
 *
 * Variables d'environnement standard Spring AMQP :
 *   SPRING_RABBITMQ_HOST, SPRING_RABBITMQ_PORT, SPRING_RABBITMQ_USERNAME, SPRING_RABBITMQ_PASSWORD
 */
@Configuration
@ConditionalOnProperty(prefix = "chaghaf.rabbitmq", name = "enabled", havingValue = "true")
public class RabbitConfig {

    public static final String NOTIFICATION_EXCHANGE   = "chaghaf.notifications";
    public static final String NOTIFICATION_QUEUE      = "chaghaf.notifications.queue";
    public static final String NOTIFICATION_ROUTING_KEY = "notification.created";

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE).build();
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(notificationQueue).to(notificationExchange).with(NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter converter) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(converter);
        return t;
    }
}
