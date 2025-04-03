package org.eclipse.slm.common.aas.clients;

import org.eclipse.digitaltwin.aas4j.v3.model.Property;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.basyx.aasregistry.client.ApiException;
import org.eclipse.digitaltwin.basyx.core.pagination.PaginationInfo;
import org.eclipse.digitaltwin.basyx.submodelrepository.client.ConnectedSubmodelRepository;
import org.eclipse.digitaltwin.basyx.submodelrepository.client.internal.SubmodelRepositoryApi;
import org.eclipse.digitaltwin.basyx.submodelservice.client.ConnectedSubmodelService;
import org.eclipse.digitaltwin.basyx.submodelservice.client.internal.SubmodelServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SubmodelServiceClient {

    private static final Logger LOG = LoggerFactory.getLogger(SubmodelServiceClient.class);

    private final ConnectedSubmodelService submodelService;

    public SubmodelServiceClient(String submodelServiceUrl, JwtAuthenticationToken jwtAuthenticationToken) {
        var apiClient = ClientUtils.getApiClient(submodelServiceUrl, jwtAuthenticationToken);
        var submodelServiceApi = new SubmodelServiceApi(apiClient);

        this.submodelService = new ConnectedSubmodelService(submodelServiceApi);
    }

    public Optional<Submodel> getSubmodel() {
        try {
            var submodel = this.submodelService.getSubmodel();

            return Optional.of(submodel);
        } catch (Exception e) {
            LOG.debug("Submodel not found or failed to get submodel", e);
            return Optional.empty();
        }
    }

    public List<SubmodelElement> getSubmodelElements() {
        var submodelElements = this.getSubmodel().get().getSubmodelElements();

        return submodelElements;
    }

    public Map<String, Object> getSubmodelValues() {
        var submodelElementValues = new HashMap<String, Object>();
        var submodelElements = this.getSubmodelElements();

        for (var submodelElement : submodelElements) {
            if (submodelElement instanceof Property) {
                submodelElementValues.put(submodelElement.getIdShort(), ((Property)submodelElement).getValue());
            }
        }

        return submodelElementValues;
    }
}
