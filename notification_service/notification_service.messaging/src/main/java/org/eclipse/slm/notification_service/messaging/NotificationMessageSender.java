package org.eclipse.slm.notification_service.messaging;

import org.eclipse.slm.common.messaging.GenericMessageSender;
import org.springframework.stereotype.Component;

@Component
public class NotificationMessageSender extends GenericMessageSender<NotificationMessage> {

    public NotificationMessageSender() throws Exception {
        super(NotificationMessage.class);
    }

}
