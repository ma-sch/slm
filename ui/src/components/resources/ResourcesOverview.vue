<template>
  <div>
    <base-material-card>
      <template #heading>
        <overview-heading text="Devices" />
      </template>

      <no-item-available-note
        v-if="!resources.length"
        item="Resource"
      />

      <v-card-text v-else>
        <v-row>
          <ResourcesTableDevices
            v-if="resources.length > 0"
            class="mt-0 flex"
            @resource-selected="onResourceSelected"
          />
        </v-row>
      </v-card-text>
    </base-material-card>

    <DeviceInfoView
      :resource="selectedResource"
      @closed="selectedResource = null"
    />
    <resources-create-dialog
      :show="showCreateDialog"
      @canceled="showCreateDialog = false"
    />
    <v-fab
      :active="!showCreateButton"
      class="mx-4"
      elevation="15"
      color="primary"
      icon="mdi-plus"
      location="right bottom"
      :app="true"
      @click="showCreateDialog = true"
    />
  </div>
</template>

<script>
import DeviceInfoView from '@/components/resources/deviceinfo/DeviceInfoView'
import ResourcesCreateDialog from '@/components/resources/dialogs/create/ResourcesCreateDialog'
import OverviewHeading from "@/components/base/OverviewHeading";
import NoItemAvailableNote from "@/components/base/NoItemAvailableNote";
import {useResourcesStore} from "@/stores/resourcesStore";
import ResourcesTableDevices from "@/components/resources/ResourcesTableDevices";

export default {
    name: 'ResourcesOverview',
    components: {
      OverviewHeading,
      DeviceInfoView,
      ResourcesCreateDialog,
      ResourcesTableDevices,
      NoItemAvailableNote,
    },
    setup(){
      const resourceStore = useResourcesStore();
      return {resourceStore};
    },
    data () {
      return {
        selectedResource: null,
        showCreateDialog: false,
        showDeleteDialog: false,
        showCreateButton: false,
      }
    },
    computed: {
      resources () {
        return this.resourceStore.resources
      },
    },
    mounted () {
      this.resourceStore.getDeploymentCapabilities();
    },
    methods: {
      getResourcesFromBackend: () => {
        return this.resourceStore.getResourcesFromBackend()
      },

      onResourceSelected (resource) {
        this.selectedResource = resource
      },
    },
  }
</script>
