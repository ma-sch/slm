package org.eclipse.slm.resource_management.service.rest.aas;

import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShellDescriptor;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.basyx.core.exceptions.ElementDoesNotExistException;
import org.eclipse.digitaltwin.basyx.http.Base64UrlEncodedIdentifier;
import org.eclipse.digitaltwin.basyx.submodelregistry.client.ApiException;
import org.eclipse.slm.common.aas.clients.*;
import org.eclipse.slm.common.consul.client.ConsulCredential;
import org.eclipse.slm.common.consul.model.exceptions.ConsulLoginFailedException;
import org.eclipse.slm.resource_management.model.resource.BasicResource;
import org.eclipse.slm.resource_management.model.resource.ResourceAas;
import org.eclipse.slm.resource_management.service.rest.aas.resources.ResourcesSubmodelRepositoryApiHTTPController;
import org.eclipse.slm.resource_management.service.rest.aas.resources.deviceinfo.DeviceInfoSubmodel;
import org.eclipse.slm.resource_management.service.rest.aas.resources.digitalnameplate.DigitalNameplateV3;
import org.eclipse.slm.resource_management.service.rest.aas.resources.digitalnameplate.DigitalNameplateV3Submodel;
import org.eclipse.slm.resource_management.service.rest.resources.ResourceEvent;
import org.eclipse.slm.resource_management.service.rest.resources.ResourcesConsulClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.annotation.PostConstruct;;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class AasHandler implements ApplicationListener<ResourceEvent> {

    private final Logger LOG = LoggerFactory.getLogger(AasHandler.class);

    private final AasRegistryClient aasRegistryClient;

    private final AasRepositoryClient aasRepositoryClient;

    private final SubmodelRegistryClient submodelRegistryClient;

    private final SubmodelRepositoryClient submodelRepositoryClient;

    private final ResourcesConsulClient resourcesConsulClient;

    private final String monitoringServiceUrl;

    private final String externalScheme;
    private final String externalHostname;
    private final String externalPort;


    public AasHandler(AasRegistryClient aasRegistryClient, AasRepositoryClient aasRepositoryClient,
                      SubmodelRegistryClient submodelRegistryClient, SubmodelRepositoryClient submodelRepositoryClient,
                      ResourcesConsulClient resourcesConsulClient,
                      @Value("${monitoring.service.url}") String monitoringServiceUrl,
                      @Value("${deployment.scheme}") String externalScheme,
                      @Value("${deployment.hostname}") String externalHostname,
                      @Value("${deployment.port}") String externalPort) {
        this.aasRegistryClient = aasRegistryClient;
        this.aasRepositoryClient = aasRepositoryClient;
        this.submodelRegistryClient = submodelRegistryClient;
        this.submodelRepositoryClient = submodelRepositoryClient;
        this.resourcesConsulClient = resourcesConsulClient;
        this.monitoringServiceUrl = monitoringServiceUrl;
        this.externalScheme = externalScheme;
        this.externalHostname = externalHostname;
        this.externalPort = externalPort;
    }

    @PostConstruct
    public void init() {
        // Create AAS for all resources
        try {
            var resources = resourcesConsulClient.getResources(new ConsulCredential());

            for (var resource: resources) {
                var digitalNameplateV3 = new DigitalNameplateV3.Builder("N/A", "N/A", "N/A", "N/A").build();
                this.createResourceAasAndSubmodels(resource, digitalNameplateV3);
            }
        } catch (ConsulLoginFailedException e) {
            throw new RuntimeException(e);
        } catch (RuntimeException e) {
            LOG.error(e.getMessage());
        }
    }

    public Optional<AssetAdministrationShellDescriptor> getResourceAasDescriptor(UUID resourceId) {
        try {
            var aasDescriptorOptional = this.aasRegistryClient.getAasDescriptor(
                    ResourceAas.createAasIdFromResourceId(resourceId));
            return aasDescriptorOptional;
        } catch (org.eclipse.digitaltwin.basyx.aasregistry.client.ApiException e) {
            LOG.error(e.getMessage());
            return Optional.empty();
        }
    }

    private String getResourcesSubmodelRepositoryUrl(Base64UrlEncodedIdentifier aasId) {
        var basePath = ResourcesSubmodelRepositoryApiHTTPController.class.getAnnotation(RequestMapping.class).value()[0];
        basePath = basePath.replace("{aasId}", aasId.getEncodedIdentifier());
        var url = this.externalScheme + "://" + this.externalHostname + ":" + this.externalPort + basePath;

        return url;
    }

    public void createResourceAasAndSubmodels(BasicResource resource, DigitalNameplateV3 digitalNameplateV3) {
        try {
            // Create AAS if it does not exist
            AssetAdministrationShell resourceAAS;
            try {
                resourceAAS = this.aasRepositoryClient.getAas(ResourceAas.createAasIdFromResourceId(resource.getId()));
            } catch (ElementDoesNotExistException e) {
                resourceAAS = new ResourceAas(resource);
                this.aasRepositoryClient.createOrUpdateAas(resourceAAS);
            }
            var resourceAASIdEncoded = new Base64UrlEncodedIdentifier(resourceAAS.getId());

            // Create submodel DigitalNameplate (if it does not exist)
            var digitalNameplateSubmodelExists = new AtomicBoolean(false);
            for (var submodelRef : resourceAAS.getSubmodels()) {
                var submodelId = submodelRef.getKeys().get(0).getValue();
                var optionalSubmodelDescriptor = this.submodelRegistryClient.findSubmodelDescriptor(submodelId);
                optionalSubmodelDescriptor.ifPresent(submodelDescriptor -> {
                    if (submodelDescriptor.getSemanticId() != null) {
                        var semanticId = submodelDescriptor.getSemanticId().getKeys().get(0).getValue();
                        if (semanticId.equals(IDTASubmodelTemplates.NAMEPLATE_V2_SUBMODEL_SEMANTIC_ID)
                                || semanticId.equals(IDTASubmodelTemplates.NAMEPLATE_V3_SUBMODEL_SEMANTIC_ID)) {
                            digitalNameplateSubmodelExists.set(true);
                            LOG.info("DigitalNameplate submodel already exists for resource [id='" + resource.getId() + "'], " +
                                    "skipping registration of digital nameplate submodel");
                        }
                    }
                });
            }

            if (!digitalNameplateSubmodelExists.get()) {
                var digitalNameplateSubmodelId = DigitalNameplateV3Submodel.SUBMODEL_IDSHORT + "-" + resource.getId();
                var digitalNameplateSubmodel = new DigitalNameplateV3Submodel(digitalNameplateSubmodelId, digitalNameplateV3);
                this.submodelRepositoryClient.createOrUpdateSubmodel(digitalNameplateSubmodel);
                this.aasRepositoryClient.addSubmodelReferenceToAas(resourceAAS.getId(), digitalNameplateSubmodelId);
            }

            // Create submodel PlatformResources
            var platformResourcesSubmodelId = "PlatformResources-" + resource.getId();
            var platformResourcesSubmodelUrl = this.monitoringServiceUrl + "/" + resource.getId() + "/submodel";
            this.aasRepositoryClient.addSubmodelReferenceToAas(resourceAAS.getId(), platformResourcesSubmodelId);
            this.submodelRegistryClient.registerSubmodel(
                    platformResourcesSubmodelUrl,
                    platformResourcesSubmodelId,
                    platformResourcesSubmodelId,
                    null);

            // Create submodel DeviceInfo
            var deviceInfoSubmodelId =  DeviceInfoSubmodel.SUBMODEL_ID_SHORT + "-" + resource.getId();
            var deviceInfoSubmodelIdEncoded = new Base64UrlEncodedIdentifier(deviceInfoSubmodelId);
            var deviceInfoSubmodelUrl = this.getResourcesSubmodelRepositoryUrl(resourceAASIdEncoded)
                    + "/submodels/" + deviceInfoSubmodelIdEncoded.getEncodedIdentifier();
            this.aasRepositoryClient.addSubmodelReferenceToAas(resourceAAS.getId(), deviceInfoSubmodelId);
            this.submodelRegistryClient.registerSubmodel(
                    deviceInfoSubmodelUrl,
                    deviceInfoSubmodelId,
                    deviceInfoSubmodelId,
                    DeviceInfoSubmodel.SEMANTIC_ID_VALUE);

        }
        catch (ApiException e) {
            LOG.error("Unable to create AAS and submodels for resource [id='" + resource.getId() + "']: " + e.getMessage());
        }
    }

    private void deleteResourceAasAndSubmodels (UUID resourceId) {
        try {
            var resourceAasId = ResourceAas.createAasIdFromResourceId(resourceId);
            var resourceAAS = this.aasRepositoryClient.getAas(resourceAasId);

            for (var submodelRef : resourceAAS.getSubmodels()) {
                if (submodelRef.getKeys().get(0).getType().equals(KeyTypes.SUBMODEL)) {
                    var submodelId = submodelRef.getKeys().get(0).getValue();

                    var submodelDescriptorOptional = this.submodelRegistryClient.findSubmodelDescriptor(submodelId);
                    if (submodelDescriptorOptional.isPresent()) {
                        var endpoint = submodelDescriptorOptional.get().getEndpoints().get(0).getProtocolInformation().getHref();

                        if (endpoint.startsWith(this.submodelRepositoryClient.getSubmodelRepositoryUrl())) {
                            this.submodelRepositoryClient.deleteSubmodel(submodelId);
                        }
                        else {
                            try {
                                this.submodelRegistryClient.unregisterSubmodel(submodelId);
                            } catch (ApiException e) {
                                LOG.error("Unable to unregister submodel [id='" + submodelId + "']: " + e.getMessage());
                            }
                        }
                    }
                }
            }
            this.aasRepositoryClient.deleteAAS(resourceAasId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void onApplicationEvent(ResourceEvent resourceEvent) {
        switch (resourceEvent.getOperation()) {
            case CREATE -> {
                // Handled in ResourcesManager.addExistingResource
            }
            case DELETE -> {
                this.deleteResourceAasAndSubmodels(resourceEvent.getResourceId());
            }
        }
    }

    @Override
    public boolean supportsAsyncExecution() {
        return ApplicationListener.super.supportsAsyncExecution();
    }
}
