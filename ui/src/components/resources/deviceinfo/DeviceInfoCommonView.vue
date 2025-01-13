<script setup lang="ts">
import {useResourcesStore} from "@/stores/resourcesStore";

const props = defineProps({
  resourceId: {
    type: String,
    default: ""
  }
});

const resourceStore = useResourcesStore();

</script>

<template>
  <v-container fluid>
    <v-table>
      <tbody>
        <tr>
          <th>{{ 'Resource ID' }}</th>
          <td colspan="3">
            {{ resourceStore.getSubmodelElementValueOfResourceSubmodel(resourceId, "DeviceInfo", "$.Id") }}
          </td>
        </tr>
        <tr>
          <th>{{ 'Hostname' }}</th>
          <td colspan="3">
            {{ resourceStore.getSubmodelElementValueOfResourceSubmodel(resourceId, "DeviceInfo", "$.Hostname") }}
          </td>
        </tr>
        <tr>
          <th>{{ 'IP' }}</th>
          <td colspan="3">
            {{ resourceStore.getSubmodelElementValueOfResourceSubmodel(resourceId, "DeviceInfo", "$.IP") }}
          </td>
        </tr>

        <!--        <tr v-if="resource.remoteAccessService !== null">-->
        <!--          <th>{{ 'Username' }}</th>-->
        <!--          <td-->
        <!--            v-if="resource.remoteAccessService"-->
        <!--            colspan="3"-->
        <!--          >-->
        <!--            {{ resource.remoteAccessService.credential.username }}-->
        <!--          </td>-->
        <!--        </tr>-->
        <!--        <tr v-if="resource.remoteAccessService !== null">-->
        <!--          <th>{{ 'Password' }}</th>-->
        <!--          <td>-->
        <!--            <v-text-field-->
        <!--              v-if="resource.remoteAccessService"-->
        <!--              :type="showPassword ? 'text' : 'password'"-->
        <!--              :model-value="resource.remoteAccessService.credential.password"-->
        <!--              disabled-->
        <!--              hide-details="auto"-->
        <!--            />-->
        <!--          </td>-->
        <!--          <td class="btn-col">-->
        <!--            <v-btn-->
        <!--              color="info"-->
        <!--              @click="togglePasswordShow"-->
        <!--            >-->
        <!--              <v-icon-->
        <!--                :icon="showPassword ? 'mdi-glasses' : 'mdi-sunglasses'"-->
        <!--                color="white"-->
        <!--              />-->
        <!--            </v-btn>-->
        <!--          </td>-->
        <!--          <td class="btn-col">-->
        <!--            <v-btn-->
        <!--              color="info"-->
        <!--              @click="copyOtp"-->
        <!--            >-->
        <!--              <v-icon icon="mdi-content-copy" />-->
        <!--            </v-btn>-->
        <!--          </td>-->
        <!--          <td-->
        <!--            v-if="resource.passwordType === 'otp'"-->
        <!--            class="btn-col"-->
        <!--          >-->
        <!--            <v-btn-->
        <!--              color="info"-->
        <!--              @click="updateOtp"-->
        <!--            >-->
        <!--              <v-icon icon="mdi-reload" />-->
        <!--            </v-btn>-->
        <!--          </td>-->
        <!--        </tr>-->
      </tbody>
    </v-table>
  </v-container>
</template>

<style scoped>
</style>