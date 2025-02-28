import {createApp} from 'vue';
import App from './App.vue'

import router from './pages/router';
import 'chartist/dist/chartist.min.css';
import 'material-design-icons-iconfont/dist/material-design-icons.css';
import setupTokenInterceptor from '@/utils/tokenInterceptor';
import VueKeycloakJs from '@dsb-norge/vue-keycloak-js'
import VueToast from 'vue-toast-notification';
import 'vue-toast-notification/dist/theme-sugar.css';
import enums from 'vue-enums';
import moment from 'moment';

import {Chart, registerables} from 'chart.js';
import {createVuetify} from "vuetify";
import cors from "cors";
import NotificationServiceWebsocketClient from "@/api/notification-service/notificationServiceWebsocketClient";

import 'vuetify/styles';
import '@/design/overrides.sass';
import upperFirst from 'lodash/upperFirst';
import camelCase from 'lodash/camelCase';

import NoItemAvailableNote from "@/components/base/NoItemAvailableNote.vue";
import {createPinia} from 'pinia';
import {useUserStore} from "@/stores/userStore";
import {useCatalogStore} from "@/stores/catalogStore";
import {useServicesStore} from "@/stores/servicesStore";
import {useProviderStore} from "@/stores/providerStore";
import {useResourcesStore} from "@/stores/resourcesStore";
import {useNotificationStore} from "@/stores/notificationStore";
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate';
import * as yup from "yup";
import {createI18n} from "vue-i18n";
import {de, en} from "vuetify/locale";
import yupValidateIPv4 from "@/utils/yup.custom";
import {VueKeycloakInstance} from "@dsb-norge/vue-keycloak-js/dist/types";
import {useEnvStore} from "@/stores/environmentStore";

const app = createApp(App);

const pinia = createPinia();
pinia.use(piniaPluginPersistedstate);
app.use(pinia);

const envStore = useEnvStore();

const components = import.meta.glob('@/components/base/*.vue');
for (const path in components) {
    components[path]().then((module) => {
        const componentName = upperFirst(
            camelCase(path.replace(/^.*[\\\/]/, '').replace(/\.\w+$/, ''))
        );
        app.component(`Base${componentName}`, module.default);
    });
}

app.component('NoItemAvailableNote', NoItemAvailableNote);

app.use(VueKeycloakJs, {
    init: {
        onLoad: 'login-required',
        checkLoginIframe: false,
    },
    config: {
        url: envStore.keycloakUrl,
        realm: envStore.keycloakRealm,
        clientId: envStore.keycloakClientId,
    },
    onInitError(error, keycloakError) {
        console.error('Keycloak init error', error, keycloakError);
    },
    onReady (keycloak) {
        keycloak.updateToken(70).then();
        setupTokenInterceptor();
        NotificationServiceWebsocketClient.connect();
        const userStore = useUserStore();
        userStore.getUserDetails().then();

        const catalogStore = useCatalogStore();
        catalogStore.updateCatalogStore().then();

        const serviceStore = useServicesStore();
        serviceStore.initServiceStore().then();
        serviceStore.getServiceInstanceGroups().then();

        const providerStore = useProviderStore();
        providerStore.getVirtualResourceProviders().then();
        providerStore.getServiceHosters().then();

        const resourceStore = useResourcesStore();
        resourceStore.getDeploymentCapabilities().then();
        resourceStore.getResourcesFromBackend().then();
        resourceStore.getResourceAASFromBackend().then();
        resourceStore.getLocations().then();
        resourceStore.getProfiler().then();
        resourceStore.getCluster().then();
        resourceStore.getClusterTypes().then();

        const notificationStore = useNotificationStore();
        notificationStore.getNotifications();
    },
});

//  Allow usage of this.$keycloak in components
declare module '@vue/runtime-core' {
    interface ComponentCustomProperties  {
        $keycloak: VueKeycloakInstance
    }
}

Chart.register(...registerables);

app.config.globalProperties.moment = moment;

app.use(enums);

const theme = {
    colors: {
        primary: '#004263',
        secondary: '#00A0E3',
        accent: '#17A6A6',
        error: '#FF7A5A',
        warning: '#F39430',
        info: '#71BD86',
        success: '#00A0E3',
        disable: '#BBBBBBD1'
    },
    variables:{
        'theme-on-info': '#fff',
    }
};

const vuetify = createVuetify({
    // lang: {
    //     t: (key, ...params) => i18n.t(key, params),
    // },
    theme: {
        defaultTheme: 'light',
        themes: {
            dark: theme,
            light: theme,
        },
    },
});
app.use(vuetify);

app.use(cors)

app.use(router);


const messages = {
    en: {
        ...require('@/localisation/en.json'),
        $vuetify: en,
    },
    de: {
        ...require('@/localisation/de.json'),
        $vuetify: de,
    },
};

const i18n = createI18n({
    globalInjection: true,
    legacy: false,
    locale: envStore.i18nLocale,
    fallbackLocale: envStore.i18nLocaleFallback,
    silentTranslationWarn: true,
    messages
});

app.use(i18n);

app.use(VueToast,{
    position: 'bottom',
    duration: 5000,
    dismissible: true,}
);

app.mount('#app');

yup.addMethod(yup.string, 'ipv4', yupValidateIPv4);

const globals = app.config.globalProperties
export { globals }
