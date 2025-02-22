<template>
  <div>
    <v-form
      v-model="validForm"
    >
      <v-list expand>
        <!-- Docker Compose File !-->
        <v-list-group :value="true">
          <template #activator>
            <v-list-item-content>
              <v-list-item-title>
                Compose File
              </v-list-item-title>
            </v-list-item-content>
          </template>
          <v-row>
            <v-col>
              <v-file-input
                v-model="uploadedComposeFile"
                accept=".yml,.yaml"
                label="Click here to select Docker Compose file"
                auto-grow
                dense
                outlined
                @change="onLoadComposeFileClicked"
              />
            </v-col>
            <v-spacer />
          </v-row>
          <v-textarea
            v-model="serviceOfferingVersion.deploymentDefinition.composeFile"
            class="full-width"
            outlined
            dense
          />
        </v-list-group>

        <!-- .env File !-->
        <v-list-group :value="false">
          <template #activator>
            <v-list-item-content>
              <v-list-item-title>
                .env File (Optional)
              </v-list-item-title>
            </v-list-item-content>
          </template>
          <v-row>
            <v-col>
              <v-file-input
                v-model="uploadedDotEnvFile"
                label="Click here to select .env file"
                outlined
                dense
                @change="onLoadDotEnvFileClicked"
              />
            </v-col>
            <v-spacer />
          </v-row>

          <v-textarea
            v-model="serviceOfferingVersion.deploymentDefinition.dotEnvFile.content"
            class="full-width"
            outlined
            dense
          />
          <docker-container-environment-variables
            :environment-variables="serviceOfferingVersion.deploymentDefinition.dotEnvFile.environmentVariables"
          />
        </v-list-group>
        <!-- Environment Variable Files !-->
        <v-list-group
          v-if="envFilesDefined"
          :value="true"
        >
          <template #activator>
            <v-list-item-content>
              <v-list-item-title>
                Environment Variable Files
              </v-list-item-title>
            </v-list-item-content>
          </template>
          <v-list>
            <v-list-group
              v-for="environmentVariableFile in serviceOfferingVersion.deploymentDefinition.envFiles"
              :key="environmentVariableFile.fileName"
              :value="true"
            >
              <template #activator>
                <v-list-item-content>
                  <v-list-item-title class="mx-8">
                    {{ environmentVariableFile.fileName }}
                  </v-list-item-title>
                </v-list-item-content>
              </template>
              <div>
                <v-row>
                  <v-col>
                    <v-file-input
                      v-model="uploadedEnvFiles[environmentVariableFile.fileName]"
                      :accept="environmentVariableFile.fileName"
                      :label="'Click here to select ' + environmentVariableFile.fileName"
                      dense
                      outlined
                      @change="onLoadEnvFileClicked(environmentVariableFile.fileName)"
                    />
                  </v-col>
                  <v-col>
                    <v-btn
                      class="noTextTransform"
                      @click="onLoadEnvFileClicked(environmentVariableFile.fileName)"
                    >
                      Load {{ environmentVariableFile.fileName }}
                    </v-btn>
                  </v-col>
                </v-row>
                <v-textarea
                  v-model="environmentVariableFile.content"
                  class="full-width mx-4"
                  outlined
                  auto-grow
                />
                <docker-container-environment-variables
                  :environment-variables="environmentVariableFile.environmentVariables"
                />
              </div>
            </v-list-group>
          </v-list>
        </v-list-group>

        <!-- Environment Variables !-->
        <v-list-group
          v-if="envVarsDefined"
          :value="true"
        >
          <template #activator>
            <v-list-item-content>
              <v-list-item-title>
                Environment
              </v-list-item-title>
            </v-list-item-content>
          </template>
          <docker-container-environment-variables
            :environment-variables="serviceOfferingVersion.deploymentDefinition.environmentVariables"
            :addable="false"
          />
        </v-list-group>
      </v-list>

      <service-repository-select
        v-model="serviceOfferingVersion.serviceRepositories"
        label="Docker Registries Credentials"
        :service-vendor-id="serviceVendorId"
        :multiple="true"
      />
    </v-form>

    <!-- Navigation Buttons-->
    <v-card-actions>
      <v-btn
        :color="$vuetify.theme.themes.light.secondary"
        @click="onCancelButtonClicked()"
      >
        {{ $t('buttons.Back') }}
      </v-btn>
      <v-spacer />
      <v-btn
        :color="$vuetify.theme.themes.light.secondary"
        @click="onNextButtonClicked()"
      >
        {{ $t('buttons.Next') }}
      </v-btn>
    </v-card-actions>
  </div>
</template>

<script>
  import DockerContainerEnvironmentVariables
    from '@/components/service_offerings/wizard_service_offering_version/Docker/DockerEnvironmentVariables'
  import 'vue-json-pretty/lib/styles.css'
  import YAML from 'yaml'
  import ServiceRepositorySelect from '@/components/service_offerings/wizard_service_offering_version/ServiceRepositorySelect'
  const { parse } = require('dot-properties')

  export default {
    name: 'ServiceOfferingVersionWizardStep2DeploymentDefinitionDockerCompose',
    components: {
      DockerContainerEnvironmentVariables,
      ServiceRepositorySelect,
    },
    props: ['editMode', 'serviceOfferingVersion', 'serviceVendorId'],

    data () {
      return {
        stepNumber: 2,
        validForm: false,
        uploadedComposeFile: null,
        uploadedDotEnvFile: null,
        uploadedEnvFiles: [],
        envVarsDefined: false,
        envFilesDefined: false,
      }
    },
    mounted() {
      if (this.editMode) {
        this.parseComposeFile(this.serviceOfferingVersion.deploymentDefinition.composeFile)
        this.serviceOfferingVersion.serviceOptionCategories.forEach(serviceOptionCategory => {
          serviceOptionCategory.serviceOptions.forEach(serviceOption => {
            if (serviceOption.optionType === "ENVIRONMENT_VARIABLE") {
              let serviceName = serviceOption.relation.split("|")[0]
              let envVarKey = serviceOption.relation.split("|")[1]
              let envVar = this.serviceOfferingVersion.deploymentDefinition.environmentVariables.find(envVar => envVar.key === envVarKey && envVar.serviceName == serviceName);
              envVar.isServiceOption = true
            }
          })
        })
      }
    },
    methods: {
      onCancelButtonClicked () {
        this.$emit('step-canceled', this.stepNumber)
      },
      onNextButtonClicked () {
        const serviceOptions = []
        this.serviceOfferingVersion.deploymentDefinition.environmentVariables.forEach((envVar) => {
          if (envVar.isServiceOption === true) {
            serviceOptions.push({
              relation: `${envVar.serviceName}`,
              key: envVar.key,
              name: envVar.key,
              description: '',
              optionType: 'ENVIRONMENT_VARIABLE',
              valueType: 'string',
              value: envVar.value,
              required: false,
              editable: false,
            })
          }
        })

        this.serviceOfferingVersion.serviceOptionCategories = []
        this.serviceOfferingVersion.serviceOptionCategories.push({
          id: 1,
          name: 'Common',
          serviceOptions: serviceOptions,
        })

        // Generate service options from .env file
        const serviceOptionsOfDotEnvFile = []
        if (this.serviceOfferingVersion.deploymentDefinition.dotEnvFile) {
          this.serviceOfferingVersion.deploymentDefinition.dotEnvFile.environmentVariables.forEach((envVarOfDotEnvFile) => {
            if (envVarOfDotEnvFile.isServiceOption === true) {
              serviceOptionsOfDotEnvFile.push({
                relation: '.env',
                key: envVarOfDotEnvFile.key,
                name: envVarOfDotEnvFile.key,
                description: '',
                optionType: 'ENVIRONMENT_VARIABLE',
                valueType: 'string',
                value: envVarOfDotEnvFile.value,
                required: false,
                editable: false,
              })
            }
          })

          if (serviceOptionsOfDotEnvFile.length > 0) {
            this.serviceOfferingVersion.serviceOptionCategories.push({
              id: this.serviceOfferingVersion.serviceOptionCategories.length + 1,
              name: '.env',
              serviceOptions: serviceOptionsOfDotEnvFile,
            })
          }
        }

        // Generate service options from env files
        for (const envFileName in this.serviceOfferingVersion.deploymentDefinition.envFiles) {
          const serviceOptionsOfEnvFile = []
          this.serviceOfferingVersion.deploymentDefinition.envFiles[envFileName].environmentVariables.forEach((envVarOfEnvFile) => {
            if (envVarOfEnvFile.isServiceOption === true) {
              serviceOptionsOfEnvFile.push({
                relation: envFileName,
                key: envVarOfEnvFile.key,
                name: envVarOfEnvFile.key,
                description: '',
                optionType: 'ENVIRONMENT_VARIABLE',
                valueType: 'string',
                value: envVarOfEnvFile.value,
                required: false,
                editable: false,
              })
            }
          })
          this.serviceOfferingVersion.serviceOptionCategories.push({
            id: this.serviceOfferingVersion.serviceOptionCategories.length + 1,
            name: envFileName,
            serviceOptions: serviceOptionsOfEnvFile,
          })
        }

        this.$emit('step-completed', this.stepNumber)
      },
      parseComposeFile (composeFileContent) {
        const parsedComposeFile = YAML.parse(composeFileContent)
        let envFilesDefined = false
        let envVarsDefined = false
        this.serviceOfferingVersion.deploymentDefinition.envFiles = {}
        this.serviceOfferingVersion.deploymentDefinition.environmentVariables = []
        for (const serviceName in parsedComposeFile.services) {
          const service = parsedComposeFile.services[serviceName]
          // Check if environment files are defined
          if ('env_file' in service) {
            service.env_file.forEach((envFileName) => {
              this.$set(this.serviceOfferingVersion.deploymentDefinition.envFiles, envFileName, {
                fileName: envFileName,
                content: '',
                environmentVariables: [],
              })

              envFilesDefined = true
            })
          }
          // Check if environment variables are defined
          if ('environment' in service) {
            for (const envVar in service.environment) {
              if (Array.isArray(service.environment)) {
                const envVarKey = service.environment[envVar].split('=')[0]
                const envVarValue = service.environment[envVar].split('=')[1]
                this.serviceOfferingVersion.deploymentDefinition.environmentVariables.push({
                  key: envVarKey,
                  value: envVarValue,
                  serviceName: serviceName,
                })
              } else {
                this.serviceOfferingVersion.deploymentDefinition.environmentVariables.push({
                  key: envVar,
                  value: service.environment[envVar],
                  serviceName: serviceName,
                })
              }
              envVarsDefined = true
            }
          }
        }
        this.envFilesDefined = envFilesDefined
        this.envVarsDefined = envVarsDefined
      },
      onLoadComposeFileClicked (files) {
        if (!this.uploadedComposeFile) {
          this.serviceOfferingVersion.deploymentDefinition.composeFile = 'No file chosen'
        } else {
          const reader = new FileReader()
          reader.readAsText(this.uploadedComposeFile)
          reader.onload = () => {
            this.serviceOfferingVersion.deploymentDefinition.composeFile = reader.result
            this.parseComposeFile(this.serviceOfferingVersion.deploymentDefinition.composeFile)
          }
        }
      },
      onLoadDotEnvFileClicked () {
        if (!this.uploadedComposeFile) {
          this.serviceOfferingVersion.deploymentDefinition.dotEnvFile = 'No File Chosen'
        } else {
          const reader = new FileReader()
          reader.readAsText(this.uploadedDotEnvFile)
          reader.onload = () => {
            this.serviceOfferingVersion.deploymentDefinition.dotEnvFile.content = reader.result
            const parsedVariables = parse(reader.result)
            for (const envVar in parsedVariables) {
              this.serviceOfferingVersion.deploymentDefinition.dotEnvFile.environmentVariables.push({
                key: envVar,
                value: parsedVariables[envVar],
              })
            }
          }
        }
      },
      onLoadEnvFileClicked (envFileName) {
        if (!this.uploadedEnvFiles[envFileName]) {
          this.serviceOfferingVersion.deploymentDefinition.envFiles[envFileName].content = 'No file chosen'
        } else {
          const reader = new FileReader()
          reader.readAsText(this.uploadedEnvFiles[envFileName])
          reader.onload = () => {
            this.serviceOfferingVersion.deploymentDefinition.envFiles[envFileName].content = reader.result
            const parsedVariables = parse(reader.result)
            for (const envVar in parsedVariables) {
              this.serviceOfferingVersion.deploymentDefinition.envFiles[envFileName].environmentVariables.push({
                key: envVar,
                value: parsedVariables[envVar],
              })
            }
          }
        }
      },
    },

  }
</script>
