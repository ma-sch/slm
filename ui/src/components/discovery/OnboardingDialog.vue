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
            Onboarding
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
            {{ discoveredResourcesIds }}
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
                Add
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
import {Form as ValidationForm} from "vee-validate";
import * as yup from "yup";

export default {
    name: 'DiscoverDialog',
    components: {
      ValidationForm
    },
    props: {
      show: {
        type: Boolean,
        default: false
      },
      discoveredResourcesIds: {
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
