package org.eclipse.slm.common.messaging.resources;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public abstract class ResourceMessageListener {

    public final static Logger LOG = LoggerFactory.getLogger(ResourceMessageListener.class);

    @Value("${spring.application.name}")
    private String nameOfReceivingService;

    public static final String QUEUE_NAME_PREFIX_RESOURCE_CREATED = "resource.created_";

    public String getResourceCreatedQueueName() {
        return ResourceMessageListener.QUEUE_NAME_PREFIX_RESOURCE_CREATED + this.nameOfReceivingService;
    }

    @Bean
    public Queue resourceCreatedQueue() {
        return new Queue(this.getResourceCreatedQueueName(), false);
    }

    @Bean
    public Binding declareBindingGeneric() {
        var resourcesExchange = new TopicExchange(ResourceMessageSender.EXCHANGE_NAME);

        return BindingBuilder.bind(resourceCreatedQueue()).to(resourcesExchange).with(ResourceCreatedMessage.ROUTING_KEY);
    }

    @RabbitListener(queues =  ResourceMessageListener.QUEUE_NAME_PREFIX_RESOURCE_CREATED + "${spring.application.name}")
    public abstract void onResourceCreated(ResourceCreatedMessage resource);

}
