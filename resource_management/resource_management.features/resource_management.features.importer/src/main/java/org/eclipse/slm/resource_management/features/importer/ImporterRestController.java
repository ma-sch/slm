package org.eclipse.slm.resource_management.features.importer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.slm.resource_management.common.aas.ResourceAas;
import org.eclipse.slm.resource_management.common.aas.ResourcesSubmodelManager;
import org.eclipse.slm.resource_management.common.aas.submodels.digitalnameplate.DigitalNameplateV3;
import org.eclipse.slm.resource_management.common.location.Location;
import org.eclipse.slm.resource_management.common.location.LocationHandler;
import org.eclipse.slm.resource_management.common.resources.ResourcesManager;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;

@RestController
@RequestMapping("/importer")
@Tag(name = "Importer")
public class ImporterRestController {

    private final ResourcesManager resourcesManager;

    private final LocationHandler locationHandler;

    private final ResourcesSubmodelManager resourcesSubmodelManager;


    public ImporterRestController(ResourcesManager resourcesManager, LocationHandler locationHandler, ResourcesSubmodelManager resourcesSubmodelManager) {
        this.resourcesManager = resourcesManager;
        this.locationHandler = locationHandler;
        this.resourcesSubmodelManager = resourcesSubmodelManager;
    }

    @RequestMapping(value = "/file", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}, method = RequestMethod.POST)
    @Operation(summary = "Import from file")
    public ResponseEntity<Void> importFromFile(
            @RequestParam(name = "file") MultipartFile importFile
    ) throws Exception {
        var jwtAuthenticationToken = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

        var importFileInputStream = new BufferedInputStream(importFile.getInputStream());

        ImportDefinition importDefinition;
        if (importFile.getOriginalFilename().contains(".xlsx")) {
            var excelImporter = new ExcelImporter();
            importDefinition = excelImporter.importExcel(importFileInputStream);
        } else if (importFile.getOriginalFilename().contains(".zip")) {
            var zipImporter = new ZipImporter();
            importDefinition = zipImporter.importZip(importFileInputStream);
        }
        else {
            return ResponseEntity.badRequest().build();
        }

        for (var location : importDefinition.getLocations().entrySet()) {
            locationHandler.addLocation(new Location(location.getKey(), location.getValue()));
        }

        for (var device : importDefinition.getDevices()) {
            var addedResource = resourcesManager.addResource(jwtAuthenticationToken,
                    device.resourceId, device.assetId, device.hostname, device.ipAddress, device.firmwareVersion, null, new DigitalNameplateV3());

            if (device.connectionPort != null
                && device.connectionType != null
                && device.username != null
                && device.password != null) {
                resourcesManager.setRemoteAccessOfResource(jwtAuthenticationToken,
                        device.resourceId,
                        device.username,
                        device.password,
                        device.connectionType,
                        device.connectionPort);
            }

            if (device.locationId != null) {
                resourcesManager.setLocationOfResource(addedResource.getId(), device.locationId);
            }
        }


        for (var aasxFilesEntry : importDefinition.getAasxFiles().entrySet()) {
            var resourceId = aasxFilesEntry.getKey();
            for (var aasxFile : aasxFilesEntry.getValue()) {
                this.resourcesSubmodelManager.addSubmodelsFromAASX(ResourceAas.createAasIdFromResourceId(resourceId), aasxFile);
            }
        }

        return ResponseEntity.ok().build();
    }
}

