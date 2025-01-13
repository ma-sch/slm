<template>
  <div>
    <v-data-table
      v-if="metrics.length > 0"
      :headers="headers"
      :items-per-page="50"
      class="elevation-1"
      item-key="name"
      :items="metrics"
      hide-default-footer
    >
      <template #item.value="{ item }">
        <div v-if="item.unit === '%'">
          {{ parseFloat(item.value).toFixed(2) }} %
        </div>
        <div v-else-if="item.unit === 'bytes'">
          {{ parseFloat(item.value/1000000000).toFixed(2) }} GB
        </div>
        <div v-else-if="item.unit === 'kilobytes'">
          {{ parseFloat(item.value/1000000).toFixed(2) }} GB
        </div>
        <div v-else-if="item.unit === 'seconds'">
          {{ parseFloat(item.value/(1000*60)).toFixed(2) }} h
        </div>
        <div v-else>
          {{ item.value }}
        </div>
      </template>
    </v-data-table>
    <div v-else>
      <v-alert
        variant="outlined"
        type="info"
      >
        No hardware information available
      </v-alert>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue';
import ResourceManagementClient from "@/api/resource-management/resource-management-client";

const props = defineProps({
  resourceId: {
    type: String,
    default: ""
  }
});

const headers = ref([
  { title: 'Category', value: 'category' },
  { title: 'Metric', value: 'name' },
  { title: 'Value', value: 'value' },
]);

const metrics = ref([]);
const pollMetrics = ref(true);

const getMetric = () => {
  ResourceManagementClient.metricsApi.getMetric(props.resourceId).then(response => {
    const metric = response.data;
    if (Object.keys(metric).length === 0) {
      return;
    }

    if (pollMetrics.value) {
      setTimeout(getMetric, 3000);
    }
    metrics.value = [];
    metrics.value.push({ category: 'System', name: 'OS', value: response.OS });
    metrics.value.push({ category: 'CPU', name: 'Architecture', value: response.ProArc });
    metrics.value.push({ category: 'CPU', name: 'Usage', value: response.CPULoad, unit: '%' });
    metrics.value.push({ category: 'RAM', name: 'Installed', value: response.SizOfTheRam, unit: 'kilobytes' });
    metrics.value.push({ category: 'RAM', name: 'Usage', value: response.AllocatedMemory, unit: '%' });
  }).catch((e) => {
    console.log(e);
    metrics.value = [];
  });
};

onMounted(() => {
  getMetric();
});

onBeforeUnmount(() => {
  pollMetrics.value = false;
});
</script>

<style scoped>
</style>
