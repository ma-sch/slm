package org.eclipse.slm.resource_management.messaging;

import org.eclipse.slm.common.messaging.AbstractMessage;

import java.io.Serializable;
import java.util.UUID;

public class ResourceCreatedMessage extends AbstractMessage implements Serializable {

    public static final String EXCHANGE_NAME = "resources";

    public static final String QUEUE_NAME_PREFIX_RESOURCE_CREATED = "resource.created@";

    public static final String ROUTING_KEY = "resource.created";

    private UUID resourceId;

    private String assetId;

    public ResourceCreatedMessage() {
        super(EXCHANGE_NAME, ROUTING_KEY, QUEUE_NAME_PREFIX_RESOURCE_CREATED);
    }

    public ResourceCreatedMessage(UUID resourceId, String assetId) {
        this();
        this.resourceId = resourceId;
        this.assetId = assetId;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public void setResourceId(UUID resourceId) {
        this.resourceId = resourceId;
    }


    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }
}
