<template>
  <v-container
    id="discovery"
    fluid
    tag="section"
  >
    <div
      v-if="apiStateLoading"
      class="text-center"
    >
      <v-progress-circular
        :size="70"
        :width="7"
        color="primary"
        indeterminate
      />
    </div>
    <div v-if="apiStateError">
      Error
    </div>

    <div v-if="apiStateLoaded">
      <discovery-overview />
    </div>
  </v-container>
</template>

<script>
import ApiState from '@/api/apiState'
import DiscoveryOverview from "@/components/discovery/DiscoveryOverview.vue";
import {useDiscoveryStore} from "@/stores/discoveryStore";

export default {
    components: {
      DiscoveryOverview,
    },
    setup(){
      const discoveryStore = useDiscoveryStore();
      return {discoveryStore};
    },
    data () {
      return {
      }
    },
    computed: {
      apiStateDiscovery() {
        return this.discoveryStore.apiStateDiscovery
      },
      apiStateLoaded () {
        return (this.apiStateDiscovery === ApiState.LOADED || this.apiStateDiscovery === ApiState.UPDATING)
      },
      apiStateLoading () {
        return this.apiStateDiscovery === ApiState.LOADING || this.apiStateDiscovery === ApiState.INIT
      },
      apiStateError () {
        return this.apiStateDiscovery === ApiState.ERROR
      },
    },
  mounted () {
    this.discoveryStore.getDrivers();
    this.discoveryStore.getDiscoveredResources();
  },
    methods: {
    },
  }
</script>

<style scoped />
