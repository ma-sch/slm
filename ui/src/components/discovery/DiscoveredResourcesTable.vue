<template>
  <v-container fluid>
    <v-row
      dense
      class="ml-8 mb-8"
      align="center"
    >
      <v-text-field
        v-model="searchDiscoveredResources"
        label="Search discovered resources"
        append-inner-icon="mdi-magnify"
        clearable
        variant="underlined"
      />
      <v-spacer />
      <v-tooltip
        text="Show ignored resources"
        open-delay="1000"
      >
        <template #activator="{ props }">
          <v-switch
            v-model="showIgnoredResources"
            v-bind="props"
            class="mx-8"
            color="primary"
            prepend-icon="mdi-eye-off-outline"
            @update:model-value="onFilterChanged"
          />
        </template>
      </v-tooltip>

      <v-tooltip
        text="Show only latest jobs of drivers"
        open-delay="1000"
      >
        <template #activator="{ props }">
          <v-switch
            v-model="showOnlyLatestJobsOfDrivers"
            v-bind="props"
            class="mx-8"
            color="primary"
            prepend-icon="mdi-history"
            @update:model-value="onFilterChanged"
          />
        </template>
      </v-tooltip>
      <v-tooltip
        text="Remove duplicates"
        open-delay="1000"
      >
        <template #activator="{ props }">
          <v-switch
            v-model="removeDuplicates"
            v-bind="props"
            class="mx-8"
            color="primary"
            prepend-icon="mdi-content-duplicate"
            @update:model-value="onFilterChanged"
          />
        </template>
      </v-tooltip>
    </v-row>
    <v-data-table
      id="table-discovered-resources"
      v-model="selectedDiscoveredResourceIds"
      :headers="tableHeaders"
      :items="discoveredResources"
      :search="searchDiscoveredResources"
      item-key="resourceId"
      show-select
      :row-props="colorRowItem"
      :loading="apiStateDiscovery === ApiState.LOADING || apiStateDiscovery === ApiState.UPDATING"
      @click:row="onRowClick"
    />
  </v-container>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue';
import { useDiscoveryStore } from "@/stores/discoveryStore";
import {storeToRefs} from "pinia";
import ApiState from "@/api/apiState";

const emit = defineEmits(['selectedDiscoveredResourcesChanged'])

const discoveryStore = useDiscoveryStore();
const { discoveredResources, apiStateDiscovery } = storeToRefs(discoveryStore);

const selectedDiscoveredResourceIds = ref([]);
const searchDiscoveredResources = ref(undefined);
const showIgnoredResources = ref(false);
const showOnlyLatestJobsOfDrivers = ref(false);
const removeDuplicates = ref(false);

const tableHeaders = [
  { title: 'Name', key: 'name', value: 'name' },
  { title: 'Product', key: 'productName', value: 'productName' },
  { title: 'Manufacturer', key: 'manufacturerName', value: 'manufacturerName' },
  { title: 'Serial number', key: 'serialNumber', value: 'serialNumber' },
  { title: 'IP address', key: 'ipAddress', value: 'ipAddress' },
  { title: 'MAC address ', key: 'macAddress', value: 'macAddress' },
  { title: 'Firmware version', key: 'firmwareVersion', value: 'firmwareVersion' },
];

watch(selectedDiscoveredResourceIds, (newVal) => {
  emit('selectedDiscoveredResourcesChanged', newVal);
});

const onRowClick = (click, row) => {
  row.toggleSelect({ value: row.item.id });
};

const colorRowItem = (row) => {
  if (selectedDiscoveredResourceIds.value.includes(row.item.id)) {
    return { class: 'v-data-table__selected' };
  }
};

const onFilterChanged = () => {
  discoveryStore.getDiscoveredResources(
    removeDuplicates.value,
    showOnlyLatestJobsOfDrivers.value,
    showIgnoredResources.value
  )
  console.log("Table:")
  console.log(discoveredResources)
};
</script>

<style>
tr.v-data-table__selected {
  background: rgb(var(--v-theme-secondary), 0.05) !important;
}
</style>