package org.eclipse.slm.notification_service.service.rest.messaging;

import org.eclipse.slm.common.messaging.GenericMessageListener;
import org.eclipse.slm.notification_service.communication.websocket.NotificationWsService;
import org.eclipse.slm.notification_service.messaging.NotificationMessage;
import org.eclipse.slm.notification_service.model.Notification;
import org.eclipse.slm.notification_service.persistence.api.NotificationRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class NotificationMessageListener extends GenericMessageListener<NotificationMessage> {

    private final NotificationRepository notificationRepository;

    private final NotificationWsService notificationWsService;

    public NotificationMessageListener(NotificationRepository notificationRepository, NotificationWsService notificationWsService) throws Exception {
        super(NotificationMessage.class);
        this.notificationRepository = notificationRepository;
        this.notificationWsService = notificationWsService;
    }

    @Override
    public void onMessageReceived(NotificationMessage notificationMessage) {
        var jwtAuthenticationToken = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

        var notification = new Notification(
                notificationMessage.getUserId(),
                notificationMessage.getTimestamp(),
                notificationMessage.getCategory(),
                notificationMessage.getSubCategory(),
                notificationMessage.getEventType(),
                notificationMessage.getPayload()
        );

//        notificationRepository.save(notification);
        notificationWsService.notifyFrontend(notification);
        LOG.info("Create new notification: " + notification);
    }


}
