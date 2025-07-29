package org.eclipse.slm.common.messaging;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public abstract class GenericMessageSender<T extends AbstractMessage> {

    public final static Logger LOG = LoggerFactory.getLogger(GenericMessageSender.class);

    private final Class<T> type;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public GenericMessageSender(Class<T> type) throws Exception {
        this.type = type;
    }

    @PostConstruct
    public void init() throws Exception {
        var tmpMessage = this.type.getDeclaredConstructor().newInstance();
        TopicExchange exchange = new TopicExchange(tmpMessage.getExchangeName());

        var amqpAdmin = new RabbitAdmin(rabbitTemplate.getConnectionFactory());
        amqpAdmin.declareExchange(exchange);
    }

    public void sendMessage(T message) {
        rabbitTemplate.convertAndSend(message.getExchangeName(), message.getRoutingKey(), message);
        LOG.info("Sent message: {}", message);
    }
}
