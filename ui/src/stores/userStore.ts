import ApiState from '@/api/apiState'
import {defineStore} from "pinia";
import {globals} from "@/main";

interface UserStoreState {
    apiStateUser_: number,

    userInfo_: null | any,
}

export const useUserStore = defineStore('userStore', {
    persist: true,
    state:():UserStoreState => ({
       apiStateUser_: ApiState.INIT,
       userInfo_: null
    }),
    getters: {
        apiStateUser: (state) => {
            return state.apiStateUser_
        },

        userId: (state) => {
            return state.userInfo_?.sub
        },
        userName: (state) => {
            return state.userInfo_.preferred_username
        },

        userInfo: (state) => {
            return state.userInfo_
        },

        userRoles(state) {
            const roles = globals.$keycloak?.realmAccess?.roles;
            if(roles === undefined){
                return [];
            }

            return roles;
        },
        isUserDeveloper(state): boolean{
            let isDeveloper = false
            this.userGroups.forEach(userGroup => {
                if (userGroup.startsWith('vendor')) {
                    isDeveloper = true
                }
            })
            if(this !== undefined){
            }

            return isDeveloper
        },
        userGroups(): any[]{

            const groups = globals.$keycloak?.tokenParsed?.groups
            if (groups === undefined) {
                return []
            } else {
                return groups
            }

            return []
        },
    },
    actions: {
        async getUserDetails () {
            this.apiStateUser_ = ApiState.LOADING;

            if (globals.$keycloak?.keycloak != undefined) {
                if (globals.$keycloak.keycloak.authenticated) {
                    await globals.$keycloak?.keycloak.loadUserInfo().then(userInfo => {
                        this.userInfo_ = userInfo;
                    })
                }
            }

            this.apiStateUser_ = ApiState.LOADED;
        },
    },
});
