<template>
  <v-dialog
    v-model="active"
    max-width="800px"
    @click:outside="closeDialog"
  >
    <template #default="{}">
      <v-toolbar
        color="primary"
        theme="dark"
      >
        <v-row
          align="center"
          justify="center"
        >
          <v-col>
            Discover resources
          </v-col>
          <v-spacer />
          <v-col
            cols="1"
          >
            <v-btn
              @click="closeDialog"
            >
              <v-icon>mdi-close</v-icon>
            </v-btn>
          </v-col>
        </v-row>
      </v-toolbar>
      <v-card>
        <ValidationForm
          ref="observer"
          v-slot="{ meta, handleSubmit, validate }"
        >
          <v-container>
            <v-row class="my-2">
              <Field
                v-slot="{ field, errors }"
                v-model="selectedDriver"
                name="resource id"
                :rules="required"
              >
                <v-select
                  v-bind="field"
                  v-model="selectedDriver"
                  class="mx-8"
                  :items="drivers"
                  item-title="instanceId"
                  return-object
                  label="Select driver to execute scan"
                  autofocus
                >
                  <template #selection="{ item }">
                    <span>{{ item.raw.name }} (Vendor: {{ item.raw.vendorName }} | Version: {{ item.raw.version }})</span>
                  </template>
                  <template #item="{ item, props }">
                    <v-list-item
                      v-bind="props"
                      :title="item.raw.name + ' (Vendor: ' + item.raw.vendorName + ' | Version: ' + item.raw.version + ')'"
                    />
                  </template>
                </v-select>
                <span>{{ errors[0] }}</span>
              </Field>
            </v-row>
          </v-container>
          <v-card-actions>
            <v-row class="mx-4">
              <v-btn
                @click="closeDialog"
              >
                Cancel
              </v-btn>
              <v-spacer />
              <v-btn
                justify="end"
                variant="text"
                :color="!meta.valid ? $vuetify.theme.themes.light.colors.disable : $vuetify.theme.themes.light.colors.secondary"
                @click="!meta.valid ? validate() : handleSubmit(onScanButtonClicked)"
              >
                Scan
              </v-btn>
            </v-row>
          </v-card-actions>
        </ValidationForm>
      </v-card>
    </template>
  </v-dialog>
</template>

<script>
import {toRef} from "vue";
import {Field, Form as ValidationForm} from "vee-validate";
import * as yup from "yup";

export default {
    name: 'DiscoverDialog',
    components: {
      Field,
      ValidationForm
    },
    props: {
      show: {
        type: Boolean,
        default: false
      },
      drivers: {
        type: Array,
        default: () => []
      }

    },
    setup(props){
      const active = toRef(props, 'show')
      const required = yup.object().shape({
          instanceId: yup.string().required("Is required")
      })
      return{
        active, required
      }
    },
    data () {
      return {
        dialog: this.active,
        selectedDriver: undefined
      }
    },
  computed: {
    yup() {
      return yup
    }
  },
    methods: {
      closeDialog () {
        this.$emit('canceled')
      },
      onScanButtonClicked () {
        this.$emit('completed', this.selectedDriver)
        this.selectedDriver = undefined
      }
    }
  }
</script>
