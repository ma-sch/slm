package org.eclipse.slm.notification_service.messaging;

import org.eclipse.slm.common.messaging.AbstractMessage;
import org.eclipse.slm.notification_service.model.*;

import java.util.Date;

public class NotificationMessage extends AbstractMessage {

    public static final String EXCHANGE_NAME = "notifications";

    public static final String QUEUE_NAME_PREFIX_RESOURCE_CREATED = "notification.created@";

    public static final String ROUTING_KEY = "notification.created";

    private String userId;

    private Date timestamp = new Date();

    private Categories category;

    private SubCategories subCategory;

    private EventType eventType;

    private Object payload;

    public NotificationMessage() {
        super(EXCHANGE_NAME, ROUTING_KEY, QUEUE_NAME_PREFIX_RESOURCE_CREATED);
    }

    public NotificationMessage(String userId,
                               Categories category,
                               SubCategories subCategory,
                               EventType eventType,
                               Object payload) {
        this();
        this.userId = userId;
        this.timestamp = new Date();
        this.category = category;
        this.subCategory = subCategory;
        this.eventType = eventType;
        this.payload = payload;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public Categories getCategory() {
        return category;
    }

    public void setCategory(Categories categories) {
        this.category = categories;
    }

    public SubCategories getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(SubCategories subCategory) {
        this.subCategory = subCategory;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }
}
