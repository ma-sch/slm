<script setup lang="ts">

import TextWithLabel from "@/components/base/TextWithLabel.vue";
import {useResourcesStore} from "@/stores/resourcesStore";

const tooltipDelay = 1000;

const props = defineProps({
  resourceId: {
    type: String,
    required: true,
  },
});

const resourceStore = useResourcesStore();

</script>

<template>
  <div>
    <v-row>
      <v-col>
        <TextWithLabel
          :label="$t('AAS.SubmodelTemplates.DigitalNameplateV3.Properties.ManufacturerName.Name')"
          :text="resourceStore.getSubmodelElementValueOfResourceSubmodel(resourceId, 'Nameplate', '$.ManufacturerName[0].en')"
          :tooltip="$t('AAS.SubmodelTemplates.DigitalNameplateV3.Properties.ManufacturerName.Description')"
          :tooltip-delay="tooltipDelay"
        />
      </v-col>
      <v-col>
        <TextWithLabel
          :label="$t('AAS.SubmodelTemplates.DigitalNameplateV3.Properties.ManufacturerProductDesignation.Name')"
          :text="resourceStore.getSubmodelElementValueOfResourceSubmodel(resourceId, 'Nameplate', '$.ManufacturerProductDesignation[0].en')"
          :tooltip="$t('AAS.SubmodelTemplates.DigitalNameplateV3.Properties.ManufacturerProductDesignation.Description')"
          :tooltip-delay="tooltipDelay"
        />
      </v-col>
      <v-col>
        <TextWithLabel
          :label="$t('AAS.SubmodelTemplates.DigitalNameplateV3.Properties.URIOfTheProduct.Name')"
          :text="resourceStore.getSubmodelElementValueOfResourceSubmodel(resourceId, 'Nameplate', '$.URIOfTheProduct')"
          :tooltip="$t('AAS.SubmodelTemplates.DigitalNameplateV3.Properties.URIOfTheProduct.Description')"
          :tooltip-delay="tooltipDelay"
        />
      </v-col>
      <v-divider />
    </v-row>
    <v-row>
      <v-col>
        <TextWithLabel
          :label="$t('AAS.SubmodelTemplates.DigitalNameplateV3.Properties.ManufacturerProductRoot.Name')"
          :text="resourceStore.getSubmodelElementValueOfResourceSubmodel(resourceId, 'Nameplate', '$.ManufacturerProductRoot[0].en')"
          :tooltip="$t('AAS.SubmodelTemplates.DigitalNameplateV3.Properties.ManufacturerProductRoot.Description')"
          :tooltip-delay="tooltipDelay"
        />
      </v-col>
      <v-col>
        <TextWithLabel
          :label="$t('AAS.SubmodelTemplates.DigitalNameplateV3.Properties.ManufacturerProductFamily.Name')"
          :text="resourceStore.getSubmodelElementValueOfResourceSubmodel(resourceId, 'Nameplate', '$.ManufacturerProductFamily[0].en')"
          :tooltip="$t('AAS.SubmodelTemplates.DigitalNameplateV3.Properties.ManufacturerProductFamily.Description')"
          :tooltip-delay="tooltipDelay"
        />
      </v-col>
      <v-col>
        <TextWithLabel
          :label="$t('AAS.SubmodelTemplates.DigitalNameplateV3.Properties.ManufacturerProductType.Name')"
          :text="resourceStore.getSubmodelElementValueOfResourceSubmodel(resourceId, 'Nameplate', '$.ManufacturerProductType')"
          :tooltip="$t('AAS.SubmodelTemplates.DigitalNameplateV3.Properties.ManufacturerProductType.Description')"
          :tooltip-delay="tooltipDelay"
        />
      </v-col>
      <v-divider />
    </v-row>
    <v-row>
      <v-col cols="4">
        <TextWithLabel
          :label="$t('AAS.SubmodelTemplates.DigitalNameplateV3.Properties.OrderCodeOfManufacturer.Name')"
          :text="resourceStore.getSubmodelElementValueOfResourceSubmodel(resourceId, 'Nameplate', '$.OrderCodeOfManufacturer')"
          :tooltip="$t('AAS.SubmodelTemplates.DigitalNameplateV3.Properties.OrderCodeOfManufacturer.Description')"
          :tooltip-delay="tooltipDelay"
        />
      </v-col>
      <v-col cols="4">
        <TextWithLabel
          :label="$t('AAS.SubmodelTemplates.DigitalNameplateV3.Properties.ProductArticleNumberOfManufacturer.Name')"
          :text="resourceStore.getSubmodelElementValueOfResourceSubmodel(resourceId, 'Nameplate', '$.ProductArticleNumberOfManufacturer')"
          :tooltip="$t('AAS.SubmodelTemplates.DigitalNameplateV3.Properties.ProductArticleNumberOfManufacturer.Description')"
          :tooltip-delay="tooltipDelay"
        />
      </v-col>
      <v-divider />
    </v-row>

    <v-row>
      <v-col>
        <TextWithLabel
          :label="$t('AAS.SubmodelTemplates.DigitalNameplateV3.Properties.SerialNumber.Name')"
          :text="resourceStore.getSubmodelElementValueOfResourceSubmodel(resourceId, 'Nameplate', '$.SerialNumber')"
          :tooltip="$t('AAS.SubmodelTemplates.DigitalNameplateV3.Properties.SerialNumber.Description')"
          :tooltip-delay="tooltipDelay"
        />
      </v-col>
      <v-col>
        <TextWithLabel
          :label="$t('AAS.SubmodelTemplates.DigitalNameplateV3.Properties.YearOfConstruction.Name')"
          :text="resourceStore.getSubmodelElementValueOfResourceSubmodel(resourceId, 'Nameplate', '$.YearOfConstruction')"
          :tooltip="$t('AAS.SubmodelTemplates.DigitalNameplateV3.Properties.YearOfConstruction.Description')"
          :tooltip-delay="tooltipDelay"
        />
      </v-col>
      <v-col>
        <TextWithLabel
          :label="$t('AAS.SubmodelTemplates.DigitalNameplateV3.Properties.DateOfManufacture.Name')"
          :text="resourceStore.getSubmodelElementValueOfResourceSubmodel(resourceId, 'Nameplate', '$.DateOfManufacture')"
          :tooltip="$t('AAS.SubmodelTemplates.DigitalNameplateV3.Properties.DateOfManufacture.Description')"
          :tooltip-delay="tooltipDelay"
        />
      </v-col>
      <v-divider />
    </v-row>

    <v-row>
      <v-col>
        <TextWithLabel
          :label="$t('AAS.SubmodelTemplates.DigitalNameplateV3.Properties.HardwareVersion.Name')"
          :text="resourceStore.getSubmodelElementValueOfResourceSubmodel(resourceId, 'Nameplate', '$.HardwareVersion')"
          :tooltip="$t('AAS.SubmodelTemplates.DigitalNameplateV3.Properties.HardwareVersion.Description')"
          :tooltip-delay="tooltipDelay"
        />
      </v-col>
      <v-col>
        <TextWithLabel
          :label="$t('AAS.SubmodelTemplates.DigitalNameplateV3.Properties.FirmwareVersion.Name')"
          :text="resourceStore.getSubmodelElementValueOfResourceSubmodel(resourceId, 'Nameplate', '$.FirmwareVersion')"
          :tooltip="$t('AAS.SubmodelTemplates.DigitalNameplateV3.Properties.FirmwareVersion.Description')"
          :tooltip-delay="tooltipDelay"
        />
      </v-col>
      <v-col>
        <TextWithLabel
          :label="$t('AAS.SubmodelTemplates.DigitalNameplateV3.Properties.SoftwareVersion.Name')"
          :text="resourceStore.getSubmodelElementValueOfResourceSubmodel(resourceId, 'Nameplate', '$.SoftwareVersion')"
          :tooltip="$t('AAS.SubmodelTemplates.DigitalNameplateV3.Properties.SoftwareVersion.Description')"
          :tooltip-delay="tooltipDelay"
        />
      </v-col>
      <v-divider />
    </v-row>

    <v-row>
      <v-col cols="4">
        <TextWithLabel
          :label="$t('AAS.SubmodelTemplates.DigitalNameplateV3.Properties.CountryOfOrigin.Name')"
          :text="resourceStore.getSubmodelElementValueOfResourceSubmodel(resourceId, 'Nameplate', '$.CountryOfOrigin')"
          :tooltip="$t('AAS.SubmodelTemplates.DigitalNameplateV3.Properties.CountryOfOrigin.Description')"
          :tooltip-delay="tooltipDelay"
        />
      </v-col>
      <v-col cols="4">
        <TextWithLabel
          :label="$t('AAS.SubmodelTemplates.DigitalNameplateV3.Properties.UniqueFacilityIdentifier.Name')"
          :text="resourceStore.getSubmodelElementValueOfResourceSubmodel(resourceId, 'Nameplate', '$.UniqueFacilityIdentifier')"
          :tooltip="$t('AAS.SubmodelTemplates.DigitalNameplateV3.Properties.UniqueFacilityIdentifier.Description')"
          :tooltip-delay="tooltipDelay"
        />
      </v-col>
      <v-row />
    </v-row>
  </div>
</template>

<style scoped>
.v-divider {
  border-color: #000000;
}
</style>