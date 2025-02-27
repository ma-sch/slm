<script setup lang="ts">

import {FirmwareUpdateStatus} from "@/api/resource-management/client";
import {PropType} from "vue";

const props = defineProps({
  firmwareUpdateStatus: {
    type: String,
    required: true,
  },
});

</script>

<template>
  <div>
    <v-icon
      v-if="firmwareUpdateStatus === FirmwareUpdateStatus.UpToDate"
      color="green"
    >
      mdi-check-circle-outline
    </v-icon>
    <v-icon
      v-else-if="firmwareUpdateStatus === FirmwareUpdateStatus.UpdateAvailable"
      color="orange"
    >
      mdi-upload-circle-outline
    </v-icon>
    <v-icon
      v-else
      color="blue"
    >
      mdi-help-circle-outline
    </v-icon>
  </div>
</template>

<style scoped>

</style>