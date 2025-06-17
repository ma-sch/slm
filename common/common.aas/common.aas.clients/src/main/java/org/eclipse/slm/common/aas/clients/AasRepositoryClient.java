package org.eclipse.slm.common.aas.clients;

import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultKey;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.eclipse.digitaltwin.basyx.aasrepository.client.ConnectedAasRepository;
import org.eclipse.digitaltwin.basyx.core.exceptions.CollidingIdentifierException;
import org.eclipse.digitaltwin.basyx.core.exceptions.CollidingSubmodelReferenceException;
import org.eclipse.digitaltwin.basyx.core.exceptions.ElementDoesNotExistException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

@Component
public class AasRepositoryClient {

    private static final Logger LOG = LoggerFactory.getLogger(AasRepositoryClient.class);

    private String aasRepositoryUrl;

    private final ConnectedAasRepository connectedAasRepository;

    private final DiscoveryClient discoveryClient;

    private final String aasRepositoryDiscoveryInstanceId = "aas-repository";

    @Autowired
    public AasRepositoryClient(@Value("${aas.aas-repository.url}") String aasRepositoryUrl,
                               DiscoveryClient discoveryClient
                               ) {
        this.aasRepositoryUrl = aasRepositoryUrl;
        this.discoveryClient = discoveryClient;

        if (discoveryClient != null) {
            var aasRepositoryServiceInstance = this.discoveryClient.getInstances(aasRepositoryDiscoveryInstanceId).get(0);
            var path = "";
            if (aasRepositoryServiceInstance.getMetadata().get("path") != null) {
                path = aasRepositoryServiceInstance.getMetadata().get("path");
            }
            if (aasRepositoryServiceInstance != null) {
                this.aasRepositoryUrl = "http://" + aasRepositoryServiceInstance.getHost()
                        + ":" + aasRepositoryServiceInstance.getPort() + path;
            } else {
                LOG.warn("No service instance '" + aasRepositoryDiscoveryInstanceId + "' found via discovery client. Using default URL '"
                        + this.aasRepositoryUrl + "' from application.yml.");
            }
        }

        this.connectedAasRepository = new ConnectedAasRepository(this.aasRepositoryUrl);
    }

    public AasRepositoryClient(String aasRepositoryUrl) {
        this(aasRepositoryUrl, null);
    }

    public void createOrUpdateAas(AssetAdministrationShell aas) {
        try {
            this.connectedAasRepository.createAas(aas);
        } catch (CollidingIdentifierException e) {
            this.connectedAasRepository.updateAas(aas.getId(), aas);
        }
        catch (RuntimeException e) {
            LOG.error(e.getMessage());
        }
    }

    public AssetAdministrationShell getAas(String aasId) {
        var aas = this.connectedAasRepository.getAas(aasId);

        return aas;
    }

    public void addSubmodelReferenceToAas(String aasId, Submodel submodel) {
        this.addSubmodelReferenceToAas(aasId, submodel.getId());
    }

    public void addSubmodelReferenceToAas(String aasId, String smId) {
        try {
            var submodelReference = new DefaultReference.Builder()
                    .keys(new DefaultKey.Builder()
                            .type(KeyTypes.SUBMODEL)
                            .value(smId).build())
                    .build();
            this.connectedAasRepository.addSubmodelReference(aasId, submodelReference);
        } catch (CollidingSubmodelReferenceException e) {
            LOG.debug("Submodel reference already exists");
        } catch (ElementDoesNotExistException e) {
            LOG.error("AAS with id {} does not exist", aasId);
        }
    }

    public void removeSubmodelReferenceFromAas(String aasId, String smId) {
        this.connectedAasRepository.removeSubmodelReference(aasId, smId);
    }

    public void deleteAAS(String aasId) {
        this.connectedAasRepository.deleteAas(aasId);
    }
}
