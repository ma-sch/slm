package org.eclipse.slm.information_service.service.impl;

import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelDescriptor;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.eclipse.slm.common.aas.clients.AasRepositoryClient;
import org.eclipse.slm.common.aas.clients.IDTASubmodelTemplates;
import org.eclipse.slm.common.aas.clients.SubmodelRegistryClient;
import org.eclipse.slm.common.aas.clients.SubmodelRepositoryClient;
import org.eclipse.slm.common.messaging.resources.ResourceCreatedMessage;
import org.eclipse.slm.common.messaging.resources.ResourceInformationFoundMessage;
import org.eclipse.slm.common.messaging.resources.ResourceMessageListener;
import org.eclipse.slm.common.messaging.resources.ResourceMessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

@Component
public class InformationServiceResourceMessageListener extends ResourceMessageListener {

    public final static Logger LOG = LoggerFactory.getLogger(InformationServiceResourceMessageListener.class);

    private final AasRepositoryClient aasRepositoryClient;
    private final SubmodelRepositoryClient submodelRepositoryClient;
    private final SubmodelRegistryClient submodelRegistryClient;
    private final ResourceMessageSender resourceMessageSender;

    private final String updateHubServiceUrl;

    public InformationServiceResourceMessageListener(@Value("${update-hub.url}") String updateHubServiceUrl,
                                                     AasRepositoryClient aasRepositoryClient,
                                                     SubmodelRepositoryClient submodelRepositoryClient,
                                                     SubmodelRegistryClient submodelRegistryClient,
                                                     ResourceMessageSender resourceMessageSender) {
        this.updateHubServiceUrl = updateHubServiceUrl;
        this.aasRepositoryClient = aasRepositoryClient;
        this.submodelRepositoryClient = submodelRepositoryClient;
        this.submodelRegistryClient = submodelRegistryClient;
        this.resourceMessageSender = resourceMessageSender;
    }

    @Override
    public void onResourceCreated(ResourceCreatedMessage resourceCreatedMessage) {
        LOG.info("Received resource created message: {}", resourceCreatedMessage);

        try {
            var aasId = "Resource_" + resourceCreatedMessage.resourceId();
            var aas = aasRepositoryClient.getAas(aasId);
            var semanticIdToSubmodelDescriptors = new HashMap<String, List<SubmodelDescriptor>>();

            // Get all submodel descriptors of submodels contained in the AAS
            for (var submodelRef : aas.getSubmodels()) {
                var submodelRefKey = submodelRef.getKeys().get(0);
                if (submodelRefKey.getType().equals(KeyTypes.SUBMODEL)) {
                    var submodelDescriptor = submodelRegistryClient.findSubmodelDescriptor(submodelRefKey.getValue());

                    if (submodelDescriptor.isPresent()) {
                        if (submodelDescriptor.get().getSemanticId() == null) {
                            LOG.info("No semantic ID found for existing submodel '{}', skipping", submodelDescriptor.get().getId());
                            continue;
                        }

                        var semanticIdKey = submodelDescriptor.get().getSemanticId().getKeys().get(0);

                        if (semanticIdToSubmodelDescriptors.containsKey(semanticIdKey.getValue())) {
                            semanticIdToSubmodelDescriptors.get(semanticIdKey.getValue())
                                    .add(submodelDescriptor.get());
                        } else {
                            semanticIdToSubmodelDescriptors.computeIfAbsent(semanticIdKey.getValue(), k -> new ArrayList<>())
                                    .add(submodelDescriptor.get());
                        }
                    }
                }
            }

            var assetId = resourceCreatedMessage.assetId();
            if (assetId == null) {
                LOG.info("Asset id for created resource '{}' not available, skipping information retrieval", resourceCreatedMessage.resourceId());
                return;
            }

            // Get submodels of device via Update Hub Service using ID Link
            var webClientBuilder = WebClient.builder();
            var webClient = webClientBuilder.baseUrl(updateHubServiceUrl)
                    .codecs(codecs -> codecs
                            .defaultCodecs()
                            .maxInMemorySize(10000 * 1024))
                    .build();

            var uriOfTheProductBase64Encoded = Base64.getEncoder().encodeToString(assetId.getBytes());
            String[] shellIds = new String[0];
            try {
                shellIds = webClient.get()
                        .uri(uriBuilder -> uriBuilder.path("/lookup/shells")
                                .queryParam("assetIds", uriOfTheProductBase64Encoded)
                                .build())
                        .retrieve()
                        .bodyToMono(String[].class)
                        .block();
            } catch (WebClientResponseException.NotFound e) {
                LOG.info("No shells found for asset id '{}', skipping information retrieval", assetId);
                return;
            }

            DefaultSubmodel[] receivedSubmodels = new DefaultSubmodel[0];
            for (var shellId : shellIds) {
                var shellIdEncoded = Base64.getEncoder().encodeToString(shellId.getBytes());
                var response = webClient.get()
                        .uri("/shells/{shellId}/submodels", shellIdEncoded)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                var aasJsonDeserializer = new JsonDeserializer();

                try {
                    receivedSubmodels = aasJsonDeserializer.read(response, DefaultSubmodel[].class);
                } catch (DeserializationException e) {
                    throw new RuntimeException(e);
                }
            }

            var duplicateSubmodelIdsToDelete = new ArrayList<String>();
            for (var submodel : receivedSubmodels) {
                if (submodel.getSemanticId() == null) {
                    LOG.info("No semantic ID found for received submodel '{}', skipping duplicate check", submodel.getId());
                }
                else {
                    var semanticId = submodel.getSemanticId().getKeys().get(0).getValue();
                    if ((semanticId.equals(IDTASubmodelTemplates.NAMEPLATE_V2_SUBMODEL_SEMANTIC_ID)
                            || semanticId.equals(IDTASubmodelTemplates.NAMEPLATE_V3_SUBMODEL_SEMANTIC_ID))
                            && (semanticIdToSubmodelDescriptors.containsKey(IDTASubmodelTemplates.NAMEPLATE_V2_SUBMODEL_SEMANTIC_ID)
                            || semanticIdToSubmodelDescriptors.containsKey(IDTASubmodelTemplates.NAMEPLATE_V3_SUBMODEL_SEMANTIC_ID))) {
                        if (semanticIdToSubmodelDescriptors.containsKey(IDTASubmodelTemplates.NAMEPLATE_V2_SUBMODEL_SEMANTIC_ID)) {
                            duplicateSubmodelIdsToDelete.add(semanticIdToSubmodelDescriptors.get(IDTASubmodelTemplates.NAMEPLATE_V2_SUBMODEL_SEMANTIC_ID).get(0).getId());
                        }
                        if (semanticIdToSubmodelDescriptors.containsKey(IDTASubmodelTemplates.NAMEPLATE_V3_SUBMODEL_SEMANTIC_ID)) {
                            duplicateSubmodelIdsToDelete.add(semanticIdToSubmodelDescriptors.get(IDTASubmodelTemplates.NAMEPLATE_V3_SUBMODEL_SEMANTIC_ID).get(0).getId());
                        }
                    }
                }

                // Add submodel to Submodel Repository
                submodelRepositoryClient.createOrUpdateSubmodel(submodel);
                // Add submodel refs to existing resourceCreatedMessage AAS
                aasRepositoryClient.addSubmodelReferenceToAas(aas.getId(), submodel.getId());
            }

            for (var submodelIdToDelete : duplicateSubmodelIdsToDelete) {
                aasRepositoryClient.removeSubmodelReferenceFromAas(aasId, submodelIdToDelete);
                submodelRepositoryClient.deleteSubmodel(submodelIdToDelete);
                LOG.info("Deleted duplicate submodel '{}' from AAS '{}'", submodelIdToDelete, aasId);
            }

            LOG.info("Successfully added {} submodels to AAS '{}'", receivedSubmodels.length, aasId);

            resourceMessageSender.sendResourceInformationMessage(resourceCreatedMessage.resourceId());
        }
        catch (Exception e) {
            Writer buffer = new StringWriter();
            PrintWriter pw = new PrintWriter(buffer);
            e.printStackTrace(pw);
            LOG.error("Error while processing resourceCreatedMessage created message: {} | Stack Trace: {}", e.getMessage(),  buffer);
        }
    }

    @Override
    public void onResourceInformationFound(ResourceInformationFoundMessage resourceInformationFoundMessage) {
        // Nothing to do
    }

}
