import {app} from "@/main";
import {defineStore} from "pinia";
import {useJobsStore} from "@/stores/jobsStore";
import {useResourcesStore} from "@/stores/resourcesStore";
import {useServicesStore} from "@/stores/servicesStore";
import NotificationServiceClient from "@/api/notification-service/notification-service-client";
import logRequestError from "@/api/restApiHelper";
import {useDiscoveryStore} from "@/stores/discoveryStore";
import {no} from "vuetify/locale";

export interface NotificationStoreState{
  notifications_: any[],
  notifications_unread_: any[],
}

export const useNotificationStore = defineStore('notificationStore', {
  persist: true,
  state: (): NotificationStoreState => ({
    notifications_: [],
    notifications_unread_: [],
  }),
  getters: {
    notifications: (state) => {
      return state.notifications_
    },
    notifications_unread: (state) => {
      return state.notifications_.filter((note) => note.read === false)
    },
  },
  actions: {

    setNotifications(notifications: any[]){
      this.notifications_ = notifications
      this.notifications_unread_ = notifications.filter(n => n.read === false)
    },

    getNotifications () {
      NotificationServiceClient.api.getNotifications().then(response => {
        this.setNotifications(response.data);
      }).catch(logRequestError);
    },

    processIncomingNotification (notification: any) {
      this.getNotifications();

      if (notification.category !== undefined) {
        app.config.globalProperties.$toast.info(notification.text)
        const resourcesStore = useResourcesStore();
        const servicesStore = useServicesStore();
        const discoveryStore = useDiscoveryStore();
        switch (notification.category) {
          case 'JOBS':
            const jobsStore = useJobsStore();

            jobsStore.updateJobsStore().then();
            resourcesStore.updateResourcesStore();
            servicesStore.updateServicesStore();
            break
          case 'RESOURCES':
            if (notification.target === 'DISCOVERY') {
              discoveryStore.updateDiscoveryStore();
            }
            else {
              resourcesStore.updateResourcesStore();
            }
            break
          case 'SERVICES':
            servicesStore.updateServicesStore();
          case 'PROJECTS':
            servicesStore.updateServicesStore();
            break
          default:
            console.debug(`Update ${notification.category} store`)
            break
        }
      }
    },
    markAsRead () {
      const unreadNotifications = this.notifications_unread_

      NotificationServiceClient.api.setReadOfNotifications(true, unreadNotifications).then(response => {
        this.getNotifications()
      }).catch(logRequestError)
    },
  },
});
