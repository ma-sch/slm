package org.eclipse.slm.resource_management.service.rest.notifications;

import org.eclipse.slm.common.messaging.GenericMessageSender;
import org.eclipse.slm.notification_service.messaging.NotificationMessage;
import org.springframework.stereotype.Component;

@Component
public class NotificationMessageSender extends GenericMessageSender<NotificationMessage> {

    public NotificationMessageSender() throws Exception {
        super(NotificationMessage.class);
    }
}
