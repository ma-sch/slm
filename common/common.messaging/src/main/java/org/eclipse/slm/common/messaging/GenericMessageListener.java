package org.eclipse.slm.common.messaging;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public abstract class GenericMessageListener<T extends AbstractMessage> implements MessageListener {

    public final static Logger LOG = LoggerFactory.getLogger(GenericMessageListener.class);

    private final Class<T> type;

    @Autowired
    private ConnectionFactory connectionFactory;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${spring.application.name}")
    private String nameOfReceivingService;

    protected GenericMessageListener(Class<T> type){
        this.type = type;
    }

    protected GenericMessageListener(Class<T> type, ConnectionFactory connectionFactory, RabbitTemplate rabbitTemplate){
        this.type = type;
        this.connectionFactory = connectionFactory;
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostConstruct
    public void init()  throws Exception  {
        var tmpMessage = this.type.getDeclaredConstructor().newInstance();

        var queueName = tmpMessage.getRoutingKey() + "@" +this.nameOfReceivingService;
        var queue = new Queue(queueName, false);;

        var amqpAdmin = new RabbitAdmin(rabbitTemplate.getConnectionFactory());
        amqpAdmin.declareQueue(queue);

        var messageListenerContainer = new SimpleMessageListenerContainer();
        messageListenerContainer.setConnectionFactory(connectionFactory);
        messageListenerContainer.setQueues(queue);
        messageListenerContainer.setMessageListener(this);
        messageListenerContainer.start();

        var resourcesExchange = new TopicExchange(tmpMessage.getExchangeName());
        var binding = BindingBuilder.bind(queue).to(resourcesExchange).with(tmpMessage.getRoutingKey());
        amqpAdmin.declareBinding(binding);
    }

    @Override
    public void onMessage(Message message) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        T typedMessage = (T) converter.fromMessage(message);
        onMessageReceived(typedMessage);
    }

    public abstract void onMessageReceived(T message);
}
