import {defineStore} from "pinia";
import {useJobsStore} from "@/stores/jobsStore";
import {useResourceDevicesStore} from "@/stores/resourceDevicesStore";
import {useResourceClustersStore} from "@/stores/resourceClustersStore";
import {useServiceInstancesStore} from "@/stores/serviceInstancesStore";
import NotificationServiceClient from "@/api/notification-service/notification-service-client";
import logRequestError from "@/api/restApiHelper";
import {useDiscoveryStore} from "@/stores/discoveryStore";
import ApiState from "@/api/apiState";
import {useToast} from "vue-toast-notification";
import {
  EventType,
  Notification,
  NotificationCategory,
  NotificationSubCategory
} from "@/api/notification-service/client";
import NotificationTextGenerator from "@/utils/notificationTextGenerator";
import i18n from '@/utils/i18n';

export interface NotificationStoreState {
  apiState: number,
  notifications: any[],
}

export const useNotificationStore = defineStore('notificationStore', {
  persist: true,

  state: (): NotificationStoreState => ({
    apiState: ApiState.INIT,
    notifications: [],
  }),

  getters: {
    notifications_unread: (state) => {
      return state.notifications.filter((note) => note.read === false)
    },
  },

  actions: {
    getNotifications () {
      NotificationServiceClient.api.getNotifications().then(response => {
        this.notifications = response.data;
      }).catch(logRequestError);
    },
    addNotification (notification: Notification) {
      this.notifications.push(notification)
    },

    processIncomingNotification (notification: Notification) {
      const $toast = useToast();
      this.addNotification(notification)

      console.log(notification)

      $toast.info(NotificationTextGenerator.generateLocalizedText(notification, i18n.global))

      const resourceDevicesStore = useResourceDevicesStore();
      const resourceClustersStore = useResourceClustersStore();
      const serviceInstancesStore = useServiceInstancesStore();

      switch (notification.category) {
        case NotificationCategory.Jobs: {
          const jobsStore = useJobsStore();

          jobsStore.updateStore();
          resourceDevicesStore.updateStore();
          resourceClustersStore.updateStore();
          serviceInstancesStore.updateStore();
          break;
        }
        case NotificationCategory.Resources: {
          handleResourcesCategoryNotification(notification);
          break;
        }
        case NotificationCategory.Services: {
          serviceInstancesStore.updateStore();
          break;
        }
        default:
          console.debug(`Update ${notification.category} store`)
          break
      }
    },
    markAllAsRead () {
      const unreadNotificationIds = this.notifications_unread.map((note) => note.id);
      NotificationServiceClient.api.setReadOfNotifications(true, unreadNotificationIds)
          .then(response => {
        this.updateStore();
      }).catch(logRequestError)
    },

    async updateStore () {
      if (this.apiState === ApiState.INIT) {
        this.apiState = ApiState.LOADING;
      } else {
        this.apiState = ApiState.UPDATING;
      }

      return Promise.all([
        this.getNotifications(),
      ]).then(() => {
        this.apiState = ApiState.LOADED;
        console.log("notificationStore updated")
      }).catch((e) => {
        this.apiState = ApiState.ERROR;
        console.log("Failed to update notificationStore: ", e)
      });
    },
  },
});

function handleResourcesCategoryNotification(notification: Notification) {
  const resourceDevicesStore = useResourceDevicesStore();
  const resourceClustersStore = useResourceClustersStore();
  const discoveryStore = useDiscoveryStore();

  switch (notification.subCategory) {
    case NotificationSubCategory.Resource: {
      switch (notification.eventType) {
        case EventType.Created: {
          resourceDevicesStore.addResource(notification.payload)
          break;
        }
        case EventType.Deleted: {
          resourceDevicesStore.deleteResource(notification.payload.id)
          break;
        }
        default: {
          resourceDevicesStore.updateStore();
          break;
        }
      }
      break;
    }
    case NotificationSubCategory.Cluster: {
      resourceClustersStore.updateStore();
      break;
    }
    case NotificationSubCategory.Discovery: {
      discoveryStore.updateDiscoveryStore();
      break;
    }
    case NotificationSubCategory.FirmwareUpdate: {
      resourceDevicesStore.getFirmwareUpdateInformationOfResource(notification.payload.resourceId, true);
      resourceDevicesStore.getFirmwareUpdateJobsOfResource(notification.payload.resourceId)
      break;
    }
    default: {
      resourceDevicesStore.updateStore();
      resourceClustersStore.updateStore();
      break;
    }
  }
}
