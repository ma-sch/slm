import {Notification, EventType, NotificationCategory, NotificationSubCategory} from "@/api/notification-service/client";

class NotificationTextGenerator {

    static generateLocalizedText(notification: Notification, t) {
        switch (notification.category) {
            case NotificationCategory.Jobs: {
                return `Job ${notification.payload} turned into state '${notification.eventType}'`
            }
            case NotificationCategory.Resources: {
                switch (notification.subCategory) {
                    case NotificationSubCategory.Resource: {
                        switch (notification.eventType) {
                            case EventType.Created: {
                                return `Resource '${notification.payload.hostname}' created`
                            }
                            case EventType.Deleted: {
                                return `Resource '${notification.payload.hostname}' deleted`
                            }
                            case EventType.Updated: {
                                return `Resource '${notification.payload.hostname}' updated`
                            }
                        }
                    }
                    case NotificationSubCategory.Capability: {
                        switch (notification.eventType) {
                            case EventType.Added: {
                                return `Capability added`
                            }
                            case EventType.Installed: {
                                return `Capability installed`
                            }
                            case EventType.Uninstalled:
                            case EventType.Deleted: {
                                return `Capability  uninstalled`
                            }
                        }
                    }
                    break;
                }
                break;
            }
            case NotificationCategory.Services: {
                return "Service"
            }

            default:
                return "Message not defined"
        }

        return `${notification.subCategory} ${notification.eventType}`
    }
}

export default NotificationTextGenerator;