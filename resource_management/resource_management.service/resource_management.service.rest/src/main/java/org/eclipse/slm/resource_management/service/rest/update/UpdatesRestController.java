package org.eclipse.slm.resource_management.service.rest.update;

import io.swagger.v3.oas.annotations.Operation;
import org.eclipse.digitaltwin.aas4j.v3.model.Property;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementCollection;
import org.eclipse.slm.common.aas.clients.AasRepositoryClient;
import org.eclipse.slm.common.aas.clients.SubmodelRegistryClient;
import org.eclipse.slm.common.aas.clients.SubmodelRepositoryClient;
import org.eclipse.slm.resource_management.model.resource.ResourceAas;
import org.eclipse.slm.resource_management.service.rest.resources.ResourcesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/resources")
public class UpdatesRestController {

    private final static Logger LOG = LoggerFactory.getLogger(UpdatesRestController.class);

    private final AasRepositoryClient aasRepositoryClient;

    private final SubmodelRegistryClient submodelRegistryClient;

    private final SubmodelRepositoryClient submodelRepositoryClient;

    private final ResourcesManager resourcesManager;

    public UpdatesRestController(AasRepositoryClient aasRepositoryClient,
                                 SubmodelRegistryClient submodelRegistryClient,
                                 SubmodelRepositoryClient submodelRepositoryClient, ResourcesManager resourcesManager) {
        this.aasRepositoryClient = aasRepositoryClient;
        this.submodelRegistryClient = submodelRegistryClient;
        this.submodelRepositoryClient = submodelRepositoryClient;
        this.resourcesManager = resourcesManager;
    }

    @RequestMapping(value = "/{resourceId}/updates", method = RequestMethod.GET)
    @Operation(summary = "Get available updates for resource")
    public ResponseEntity<UpdateInformation> getUpdateInformationOfResource(
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

        var availableFirmwareVersions = new ArrayList<FirmwareVersionDetails>();
        for (var softwareNameplateSubmodel : softwareNameplateSubmodels) {
            softwareNameplateSubmodel.getSubmodelElements()
                    .stream().filter(se -> se.getIdShort().equals("SoftwareNameplateType"))
                    .findAny().ifPresent(
                            softwareNameplateTypeSmc -> {

                                var firmwareVersionDetails = new FirmwareVersionDetails.Builder();
                                firmwareVersionDetails.softwareNameplateSubmodelId(softwareNameplateSubmodel.getId());

                                ((SubmodelElementCollection) softwareNameplateTypeSmc).getValue()
                                        .stream().filter(se -> se.getIdShort().equals("Version"))
                                        .findAny()
                                        .ifPresent(
                                                prop -> {
                                                    var version = ((Property) prop).getValue();
                                                    firmwareVersionDetails.version(version);
                                                }
                                        );

                                ((SubmodelElementCollection) softwareNameplateTypeSmc).getValue()
                                        .stream().filter(se -> se.getIdShort().equals("ReleaseDate"))
                                        .findAny()
                                        .ifPresent(
                                                prop -> {
                                                    var dateString = ((Property) prop).getValue();
                                                    firmwareVersionDetails.date(dateString);
                                                }
                                        );

                                ((SubmodelElementCollection) softwareNameplateTypeSmc).getValue()
                                        .stream().filter(se -> se.getIdShort().equals("InstallationURI"))
                                        .findAny()
                                        .ifPresent(
                                                prop -> {
                                                    var installationUri = ((Property) prop).getValue();
                                                    firmwareVersionDetails.installationUri(installationUri);
                                                }
                                        );

                                ((SubmodelElementCollection) softwareNameplateTypeSmc).getValue()
                                        .stream().filter(se -> se.getIdShort().equals("InstallationChecksum"))
                                        .findAny()
                                        .ifPresent(
                                                prop -> {
                                                    var installationChecksum = ((Property) prop).getValue();
                                                    firmwareVersionDetails.installationChecksum(installationChecksum);
                                                }
                                        );

                                availableFirmwareVersions.add(firmwareVersionDetails.build());
                            }
                    );
        }

        // Sort available firmware versions from new to old
        availableFirmwareVersions.sort((o1, o2) -> {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            try {
                var date1 = formatter.parse(o1.getDate());
                var date2 = formatter.parse(o2.getDate());

                return date2.compareTo(date1);
            } catch (ParseException e) {
                LOG.error("Error parsing date", e);
            }

            return 0;
        });

        var updateInformation = new UpdateInformation();
        updateInformation.setFirmwareUpdateStatus(FirmwareUpdateStatus.UNKNOWN);
        updateInformation.setAvailableFirmwareVersions(availableFirmwareVersions);

        FirmwareVersionDetails currentFirmwareVersion = null;
        var resourceOptional = resourcesManager.getResourceWithoutCredentials(resourceId);
        if (resourceOptional.isPresent()) {

            for (int i = 0; i < availableFirmwareVersions.size(); i++) {
                var firmwareVersion = availableFirmwareVersions.get(i);
                if (firmwareVersion.getVersion().equals(resourceOptional.get().getFirmwareVersion())) {
                    currentFirmwareVersion = firmwareVersion;
                    // If current firmware version is on top of sorted list of availableFirmwareVersions, it is up to date
                    if (i == 0) {
                        updateInformation.setFirmwareUpdateStatus(FirmwareUpdateStatus.UP_TO_DATE);
                    }
                    else {
                        updateInformation.setFirmwareUpdateStatus(FirmwareUpdateStatus.UPDATE_AVAILABLE);
                    }
                }
            }

            if (currentFirmwareVersion == null) {
                currentFirmwareVersion = new FirmwareVersionDetails(resourceOptional.get().getFirmwareVersion(), "", "", "", "");
            }
            updateInformation.setCurrentFirmwareVersion(currentFirmwareVersion);
        }

        updateInformation.setLatestFirmwareVersion(availableFirmwareVersions.get(0));

        return ResponseEntity.ok(updateInformation);
    }

}
