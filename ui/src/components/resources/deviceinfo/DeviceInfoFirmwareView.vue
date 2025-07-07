<script setup lang="ts">

import ResourceManagementClient from "@/api/resource-management/resource-management-client";
import logRequestError from "@/api/restApiHelper";
import {computed, onMounted, ref} from "vue";
import {UpdateInformation} from "@/api/resource-management/client";
import ProgressCircular from "@/components/base/ProgressCircular.vue";
import RowWithLabel from "@/components/base/RowWithLabel.vue";
import FirmwareUpdateStatusIcon from "@/components/updates/FirmwareUpdateStatusIcon.vue";
import formatDate from "@/utils/dateUtils";
import axios from 'axios'

const props = defineProps({
  resourceId: {
    type: String,
    required: true,
  },
});

const loading = ref(false);
const updateInformation = ref<UpdateInformation>({});
onMounted(() => {
  loading.value = true;
  ResourceManagementClient.resourcesUpdatesApi.getUpdateInformationOfResource(props.resourceId)
      .then(
          response => {
            updateInformation.value = response.data;
            loading.value = false;
          }
      ).catch(e => {
        logRequestError(e)
        loading.value = false;
      }
  )
});

const tableHeadersFiles = [
  { title: 'Name', key: 'fileName', value: 'fileName' },
  { title: 'Size', key: 'fileSize', value: 'fileSizeBytes' },
  { title: 'Date', key: 'fileUploadDate', value: 'uploadDate' },
  { title: 'Actions', key: 'fileActions', value: 'fileActions' },
];

const installedVersionText = computed(() => {
  const version = updateInformation.value.currentFirmwareVersion?.version;
  if (!version) {
    return "N/A"
  }
  const date = updateInformation.value.currentFirmwareVersion?.date;
  return date ? `${version} (${formatDate(date)})` : version;
});

function humanFileSize(size) {
  var i = size == 0 ? 0 : Math.floor(Math.log(size) / Math.log(1024));
  return +((size / Math.pow(1024, i)).toFixed(2)) * 1 + ' ' + ['B', 'kB', 'MB', 'GB', 'TB'][i];
}

function downloadItem (item) {
  axios.get(item.downloadUrl, { responseType: 'blob' })
      .then(response => {
        const blob = new Blob([response.data])
        const link = document.createElement('a')
        link.href = URL.createObjectURL(blob)
        link.download = item.fileName
        link.click()
        URL.revokeObjectURL(link.href)
      }).catch(console.error)
}

</script>

<template>
  <div>
    <div v-if="loading">
      <progress-circular />
    </div>

    <div v-else>
      <RowWithLabel
        label="Installed version"
        :text="installedVersionText"
      />

      <RowWithLabel
        label="Update status"
        :text="updateInformation.firmwareUpdateStatus"
      >
        <template #content>
          <FirmwareUpdateStatusIcon
            :firmware-update-status="updateInformation.firmwareUpdateStatus"
            :clickable="false"
          />
        </template>
      </RowWithLabel>

      <RowWithLabel
        label="Available versions"
        :divider="false"
      />
      <v-expansion-panels
        variant="accordion"
        flat
      >
        <v-expansion-panel
          v-for="firmwareVersion in updateInformation.availableFirmwareVersions"
          :key="firmwareVersion.version"
          :title="firmwareVersion.version"
          expand
        >
          <template #text>
            <RowWithLabel
              label="Version"
              :text="firmwareVersion.version"
            />
            <RowWithLabel
              label="Date"
              :text="firmwareVersion.date"
            />
            <RowWithLabel
              label="Installation URI"
            >
              <template #content>
                <a
                  :href="firmwareVersion.installationUri"
                  target="_blank"
                >{{ firmwareVersion.installationUri }}</a>
              </template>
            </RowWithLabel>
            <RowWithLabel
              label="Checksum"
              :text="firmwareVersion.installationChecksum"
            />
            <RowWithLabel
              label="File"
            >
              <template #content>
                <v-data-table
                  v-if="firmwareVersion.firmwareUpdateFile"
                  :headers="tableHeadersFiles"
                  :items="[ firmwareVersion.firmwareUpdateFile ]"
                  hide-default-footer
                >
                  <template #item.fileSize="{ item }">
                    {{ humanFileSize(item.fileSizeBytes) }}
                  </template>
                  <template #item.fileUploadDate="{ item }">
                    {{ formatDate(item.uploadDate) }}
                  </template>
                  <template #item.fileActions="{ item }">
                    <v-icon
                      class="ml-4"
                      color="secondary"
                      @click.prevent="downloadItem(item)"
                    >
                      mdi-download
                    </v-icon>

                    <!--                    <v-icon-->
                    <!--                      class="ml-4"-->
                    <!--                      color="error"-->
                    <!--                      @click="deleteItem(firmwareVersion.softwareNameplateSubmodelId)"-->
                    <!--                    >-->
                    <!--                      mdi-delete-->
                    <!--                    </v-icon>-->
                  </template>
                </v-data-table>
                <div v-else>
                  No file available
                </div>
              </template>
            </RowWithLabel>
          </template>
        </v-expansion-panel>
      </v-expansion-panels>
    </div>
  </div>
</template>