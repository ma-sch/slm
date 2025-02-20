package org.eclipse.slm.resource_management.service.rest.update;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.type.Date;
import io.swagger.v3.oas.annotations.Operation;
import org.eclipse.digitaltwin.aas4j.v3.model.Property;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementCollection;
import org.eclipse.slm.common.aas.clients.AasRepositoryClient;
import org.eclipse.slm.common.aas.clients.SubmodelRegistryClient;
import org.eclipse.slm.common.aas.clients.SubmodelRepositoryClient;
import org.eclipse.slm.common.consul.model.exceptions.ConsulLoginFailedException;
import org.eclipse.slm.resource_management.model.resource.ResourceAas;
import org.eclipse.slm.resource_management.model.resource.exceptions.ResourceNotFoundException;
import org.hibernate.sql.Update;
import org.joda.time.LocalDateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/resources")
public class UpdatesRestController {

    private final static Logger LOG = LoggerFactory.getLogger(UpdatesRestController.class);

    private final AasRepositoryClient aasRepositoryClient;

    private final SubmodelRegistryClient submodelRegistryClient;

    private final SubmodelRepositoryClient submodelRepositoryClient;

    public UpdatesRestController(AasRepositoryClient aasRepositoryClient,
                                 SubmodelRegistryClient submodelRegistryClient,
                                 SubmodelRepositoryClient submodelRepositoryClient) {
        this.aasRepositoryClient = aasRepositoryClient;
        this.submodelRegistryClient = submodelRegistryClient;
        this.submodelRepositoryClient = submodelRepositoryClient;
    }

    @RequestMapping(value = "/{resourceId}/updates", method = RequestMethod.GET)
    @Operation(summary = "Get available updates for resource")
    public ResponseEntity<List<UpdateInformation>> geAvailableUpdatesOfResource(
            @PathVariable(name = "resourceId") UUID resourceId
    ) {

        var resourceAas = aasRepositoryClient.getAas(ResourceAas.createAasIdFromResourceId(resourceId));
        var softwareNameplateSubmodels = new ArrayList<Submodel>();
        for (var submodelRef : resourceAas.getSubmodels()) {
            var submodelId = submodelRef.getKeys().get(0).getValue();

            this.submodelRegistryClient.findSubmodelDescriptor(submodelId).ifPresent(submodelDescriptor -> {
                if (submodelDescriptor.getIdShort().contains("SoftwareNameplate")) {
                    var submodel = this.submodelRepositoryClient.getSubmodel(submodelDescriptor.getId());
                    softwareNameplateSubmodels.add(submodel);
                }
            });
        }

        var updates = new ArrayList<UpdateInformation>();
        for (var softwareNameplateSubmodel : softwareNameplateSubmodels) {
            softwareNameplateSubmodel.getSubmodelElements()
                    .stream().filter(se -> se.getIdShort().equals("SoftwareNameplateType"))
                    .findAny().ifPresent(
                            softwareNameplateTypeSmc -> {

                                var updateInformation = new UpdateInformation.Builder();

                                ((SubmodelElementCollection) softwareNameplateTypeSmc).getValue()
                                        .stream().filter(se -> se.getIdShort().equals("Version"))
                                        .findAny()
                                        .ifPresent(
                                                prop -> {
                                                    var version = ((Property) prop).getValue();
                                                    updateInformation.setVersion(version);
                                                }
                                        );

                                ((SubmodelElementCollection) softwareNameplateTypeSmc).getValue()
                                        .stream().filter(se -> se.getIdShort().equals("ReleaseDate"))
                                        .findAny()
                                        .ifPresent(
                                                prop -> {
                                                    var dateString = ((Property) prop).getValue();
                                                    updateInformation.setDate(dateString);
                                                }
                                        );

                                ((SubmodelElementCollection) softwareNameplateTypeSmc).getValue()
                                        .stream().filter(se -> se.getIdShort().equals("InstallationURI"))
                                        .findAny()
                                        .ifPresent(
                                                prop -> {
                                                    var installationUri = ((Property) prop).getValue();
                                                    updateInformation.setInstallationUri(installationUri);
                                                }
                                        );

                                ((SubmodelElementCollection) softwareNameplateTypeSmc).getValue()
                                        .stream().filter(se -> se.getIdShort().equals("InstallationChecksum"))
                                        .findAny()
                                        .ifPresent(
                                                prop -> {
                                                    var installationChecksum = ((Property) prop).getValue();
                                                    updateInformation.setInstallationChecksum(installationChecksum);
                                                }
                                        );

                                updates.add(updateInformation.build());
                            }
                    );
        }

        return ResponseEntity.ok(updates);
    }

}
