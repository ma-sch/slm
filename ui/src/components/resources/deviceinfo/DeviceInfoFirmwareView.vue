<script setup lang="ts">

import ResourceManagementClient from "@/api/resource-management/resource-management-client";
import logRequestError from "@/api/restApiHelper";
import {computed, onMounted, Ref, ref} from "vue";
import {FirmwareUpdateJob, UpdateInformation} from "@/api/resource-management/client";
import ProgressCircular from "@/components/base/ProgressCircular.vue";
import RowWithLabel from "@/components/base/RowWithLabel.vue";
import FirmwareUpdateStatusIcon from "@/components/updates/FirmwareUpdateStatusIcon.vue";
import formatDate, {formatDateTime} from "@/utils/dateUtils";
import axios from 'axios'
import ConfirmDialog from "@/components/base/ConfirmDialog.vue";
import {useToast} from "vue-toast-notification";
import {storeToRefs} from "pinia";
import {useResourceDevicesStore} from "@/stores/resourceDevicesStore";

const $toast = useToast();

const props = defineProps({
  resourceId: {
    type: String,
    required: true,
  },
});

const resourceDevicesStore = useResourceDevicesStore();

const loading = ref(false);
const {firmwareUpdateInformationOfResource, firmwareUpdateJobsOfResource} = storeToRefs(resourceDevicesStore)

onMounted(() => {
  loading.value = true;
  resourceDevicesStore.getFirmwareUpdateInformationOfResource(props.resourceId).then(
      response => {
        loading.value = false;
      }
  ).catch(e => {
    loading.value = false;
  });
  resourceDevicesStore.getFirmwareUpdateJobsOfResource(props.resourceId);
});

const filesTableHeaders = [
  { title: 'Name', key: 'fileName', value: 'fileName' },
  { title: 'Size', key: 'fileSize', value: 'fileSizeBytes' },
  { title: 'Date', key: 'fileUploadDate', value: 'uploadDate' },
  { title: 'Actions', key: 'fileActions', value: 'fileActions' },
];

const jobTableHeaders = [
  { title: 'Created', key: 'create', value: 'createdAt', width: "20%" },
  { title: 'Target Version', key: 'version', value: 'version', width: "20%" },
  { title: 'State', key: 'state',  value: 'firmwareUpdateState', width: "20%" },
  { title: 'Job Id', key: 'id', value: 'id', width: "40%" },
];

const installedVersionText = computed(() => {
  const version = firmwareUpdateInformationOfResource.value(props.resourceId).currentFirmwareVersion?.version;
  if (!version) {
    return "N/A"
  }
  const date = firmwareUpdateInformationOfResource.value(props.resourceId).currentFirmwareVersion?.date;
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

const selectedFirmwareVersion = ref(undefined);
const showConfirmFirmwareUpdateInstallation = ref(false);
function installFirmwareUpdate() {
  let softwareNameplateSubmodelIdBase64Encoded = btoa(selectedFirmwareVersion.value.softwareNameplateSubmodelId);
  ResourceManagementClient.resourcesUpdatesApi.startFirmwareUpdateOnResource(
      props.resourceId,
      softwareNameplateSubmodelIdBase64Encoded
  ).then(() => {
    // Reload jobs
    resourceDevicesStore.getFirmwareUpdateJobsOfResource(props.resourceId);
  });

  $toast.success("Firmware update started");

  console.log(selectedFirmwareVersion);
  selectedFirmwareVersion.value = undefined;
}

function getVersionTextOfSoftwareNameplate(softwareNameplateSubmodelId) {
  const version = firmwareUpdateInformationOfResource.value(props.resourceId).availableFirmwareVersions?.find(
      firmwareVersion =>
          firmwareVersion.softwareNameplateSubmodelId?.trim() === softwareNameplateSubmodelId?.trim()
  )?.version;
  return version ?? "N/A";
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
      >
        <template #content>
          <div v-if="firmwareUpdateInformationOfResource(props.resourceId).isUpdateInProgress">
            <progress-circular
              size="20"
              width="2"
            />
            <v-chip
              v-if="firmwareUpdateInformationOfResource(props.resourceId).isUpdateInProgress"
              color="primary"
              variant="elevated"
              class="mx-8"
              label
              size="small"
            >
              {{ firmwareUpdateJobsOfResource(props.resourceId)[0].firmwareUpdateState }}
            </v-chip>
          </div>

          <FirmwareUpdateStatusIcon
            v-else
            :firmware-update-status="firmwareUpdateInformationOfResource(props.resourceId).firmwareUpdateStatus"
            :clickable="false"
          />
        </template>
      </RowWithLabel>

      <RowWithLabel
        label="Available versions"
        :divider="true"
      >
        <template #content>
          <div v-if="firmwareUpdateInformationOfResource(props.resourceId).availableFirmwareVersions?.length == 0">
            No updates available
          </div>
          <v-expansion-panels
            v-else
            variant="accordion"
            flat
          >
            <v-expansion-panel
              v-for="firmwareVersion in firmwareUpdateInformationOfResource(props.resourceId).availableFirmwareVersions"
              :key="firmwareVersion.version"
              expand
            >
              <template #title>
                <div style="display: flex; align-items: center; justify-content: space-between; width: 100%;">
                  <v-row>
                    <v-col cols="11">
                      <span>{{ firmwareVersion.version }}</span>
                    </v-col>
                    <v-col cols="1">
                      <v-btn
                        v-if="firmwareVersion.version === firmwareUpdateInformationOfResource(props.resourceId)?.currentFirmwareVersion?.version"
                        color="secondary"
                        size="small"
                      >
                        Installed
                      </v-btn>
                      <div v-else>
                        <v-btn
                          v-if="firmwareVersion.firmwareUpdateFile"
                          color="primary"
                          size="small"
                          :disabled="firmwareUpdateInformationOfResource(props.resourceId).isUpdateInProgress"
                          @click.stop="selectedFirmwareVersion = firmwareVersion; showConfirmFirmwareUpdateInstallation = true;"
                        >
                          Install
                          <ConfirmDialog
                            :show="showConfirmFirmwareUpdateInstallation"
                            :title="`Install firmware update`"
                            :text="`Do you want to update the firmware of the device to version '${selectedFirmwareVersion?.version}'?`"
                            @confirmed="showConfirmFirmwareUpdateInstallation = false; installFirmwareUpdate()"
                            @canceled="showConfirmFirmwareUpdateInstallation = false"
                          />
                        </v-btn>
                      </div>
                    </v-col>
                  </v-row>
                </div>
              </template>
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
                      :headers="filesTableHeaders"
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
        </template>
      </RowWithLabel>

      <RowWithLabel
        label="Update jobs"
        :divider="true"
      >
        <template #content>
          <div v-if="firmwareUpdateJobsOfResource(props.resourceId)?.length === 0">
            No jobs available
          </div>
          <v-data-table
            v-else
            :headers="jobTableHeaders"
            :items="firmwareUpdateJobsOfResource(props.resourceId)"
            item-key="id"
            :sort-by="[{ key: 'createdAt', order: 'desc' }]"
            show-expand
            class="elevation-0"
          >
            <template #item.state="{ item }">
              <span>{{ item.firmwareUpdateState }}</span>
            </template>
            <template #item.version="{ item }">
              <span>{{ getVersionTextOfSoftwareNameplate(item.softwareNameplateId) }}</span>
            </template>
            <template #item.create="{ item }">
              <span>{{ formatDateTime(item.createdAt) }}</span>
            </template>
            <template #expanded-row="{ item }">
              <td
                class="ma-4"
                :colspan="jobTableHeaders.length+1"
              >
                <div
                  v-if="item.stateTransitions?.length > 0"
                  class="mx-4"
                >
                  <v-timeline direction="horizontal">
                    <v-timeline-item
                      v-for="transition in item.stateTransitions"
                      :key="transition.id"
                      dot-color="primary"
                    >
                      <div>
                        <strong>{{ transition.toState }}</strong>
                        <div>{{ formatDateTime(transition.timestamp) }}</div>
                      </div>
                    </v-timeline-item>
                  </v-timeline>
                </div>
                <div v-else>
                  No state transitions
                </div>
              </td>
            </template>
          </v-data-table>
        </template>
      </RowWithLabel>
    </div>
  </div>
</template>

<style scoped>
.v-divider {
  border-color: #000000;
}

.non-clickable {
  pointer-events: none;
  opacity: 0.5;
}
</style>