<script setup lang="ts">

import ResourceManagementClient from "@/api/resource-management/resource-management-client";
import logRequestError from "@/api/restApiHelper";
import {onMounted, ref} from "vue";
import {UpdateInformation} from "@/api/resource-management/client";
import ProgressCircular from "@/components/base/ProgressCircular.vue";

const props = defineProps({
  resourceId: {
    type: String,
    required: true,
  },
});

const loading = ref(false);
const updates = ref<Array<UpdateInformation>>([]);
onMounted(() => {
  loading.value = true;
  ResourceManagementClient.resourcesUpdatesApi.geAvailableUpdatesOfResource(props.resourceId)
      .then(
          response => {
            console.log(response.data)
            updates.value = response.data;
            loading.value = false;
          }
      ).catch(e => {
        logRequestError(e)
        loading.value = false;
      }
  )
});

const tableHeaders = [
  { title: 'Version', key: 'version', value: 'version' },
  { title: 'Date', key: 'date', value: 'date' },
  { title: 'Installation URI', key: 'installationUri', value: 'installationUri' },
  { title: 'Installation Checksum', key: 'installationChecksum', value: 'installationChecksum' },
];

</script>

<template>
  <div>
    <div v-if="loading">
      <progress-circular />
    </div>

    <v-data-table
      v-else
      :headers="tableHeaders"
      :items-per-page="10"
      class="elevation-1"
      item-key="version"
      :items="updates"
    >
      <template #item.installationUri="{ item }">
        <a
          :href="item.installationUri"
          target="_blank"
        >{{ item.installationUri }}</a>
      </template>
    </v-data-table>
  </div>
</template>