import ApiState from '@/api/apiState'
import {defineStore} from "pinia";
import ResourceManagementClient from "@/api/resource-management/resource-management-client";
import logRequestError from "@/api/restApiHelper";

interface DiscoveryStoreState{
    apiStateDiscovery_: number,

    drivers_: any[],
    discoveredResources_: any[],
}

export const useDiscoveryStore = defineStore('discoveryStore', {

    persist: true,
    state:():DiscoveryStoreState => ({
        apiStateDiscovery_: ApiState.INIT,

        drivers_: [],
        discoveredResources_: [],
    }),
    getters: {
        apiStateDiscovery(state) {
            return state.apiStateDiscovery_
        },
        drivers: (state) => {
            return state.drivers_
        },
        discoveredResources: (state) => {
            return state.discoveredResources_
        },
    },

    actions: {
        async updateDiscoveryStore () {
            console.log("Update discovery store")
            this.getDrivers().then();
            this.getDiscoveredResources().then();
        },

        async getDrivers () {
            if (this.apiStateDiscovery_ === ApiState.INIT) {
                this.apiStateDiscovery_ = ApiState.LOADING;
            } else {
                this.apiStateDiscovery_ = ApiState.UPDATING;
            }

            return await ResourceManagementClient.discoveryApi.getRegisteredDrivers().then(
                response => {
                    this.drivers_ = response.data;
                    this.apiStateDiscovery_ = ApiState.LOADED;
                }
            ).catch(logRequestError)
        },

        async getDiscoveredResources (removeDuplicate = false, onlyLatestJobs = false, includeIgnored = false) {
            if (this.apiStateDiscovery_ === ApiState.INIT) {
                this.apiStateDiscovery_ = ApiState.LOADING;
            } else {
                this.apiStateDiscovery_ = ApiState.UPDATING;
            }

            return await ResourceManagementClient.discoveryApi.getDiscoveredResources(removeDuplicate, onlyLatestJobs, includeIgnored).then(
                response => {
                    this.discoveredResources_ = response.data;
                    this.apiStateDiscovery_ = ApiState.LOADED;
                }
            ).catch(logRequestError)
        },
    },
});

