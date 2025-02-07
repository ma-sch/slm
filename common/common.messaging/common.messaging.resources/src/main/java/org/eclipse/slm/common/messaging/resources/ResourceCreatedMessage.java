package org.eclipse.slm.common.messaging.resources;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.UUID;

public record ResourceCreatedMessage(
        @JsonProperty("getResourceId") UUID getResourceId
) implements Serializable {

    public static final String ROUTING_KEY = "resource.created";

    public UUID getResourceId() {
        return getResourceId;
    }
}
