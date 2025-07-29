package org.eclipse.slm.resource_management.service.rest.resources;

import org.eclipse.slm.common.messaging.GenericMessageSender;
import org.eclipse.slm.resource_management.messaging.ResourceCreatedMessage;
import org.springframework.stereotype.Component;

@Component
public class ResourceCreatedMessageSender extends GenericMessageSender<ResourceCreatedMessage> {

    public ResourceCreatedMessageSender() throws Exception {
        super(ResourceCreatedMessage.class);
    }

}
