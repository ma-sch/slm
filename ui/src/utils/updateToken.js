import {globals} from "@/main";

export default async function () {
    if(globals.$keycloak?.keycloak?.updateToken !== undefined) {
        await globals.$keycloak.keycloak.updateToken(70);
    }
    return globals.$keycloak;
}
