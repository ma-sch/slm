package org.eclipse.slm.common.messaging;

import java.io.Serializable;

public abstract class AbstractMessage implements Serializable {

    private final String exchangeName;
    private final String routingKey;

    protected AbstractMessage(String exchangeName, String routingKey, String queueNamePrefix) {
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
    }

    public String getExchangeName() {
        return this.exchangeName;
    }

    public String getRoutingKey() {
        return this.routingKey;
    }

}
