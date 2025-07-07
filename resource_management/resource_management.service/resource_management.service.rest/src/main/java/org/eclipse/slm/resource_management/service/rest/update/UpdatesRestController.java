package org.eclipse.slm.resource_management.service.rest.update;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.io.IOUtils;
import org.eclipse.slm.common.minio.model.exceptions.*;
import org.eclipse.slm.resource_management.model.resource.exceptions.ResourceTypeNotFoundException;
import org.eclipse.slm.resource_management.model.update.UpdateInformationResource;
import org.eclipse.slm.resource_management.model.update.UpdateInformationResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/resources")
@Tag(name = "Updates")
public class UpdatesRestController {

    private final static Logger LOG = LoggerFactory.getLogger(UpdatesRestController.class);

    private final UpdateManager updateManager;

    public UpdatesRestController(UpdateManager updateManager) {
        this.updateManager = updateManager;
    }

    @RequestMapping(value = "/{resourceId}/updates", method = RequestMethod.GET)
    @Operation(summary = "Get available updates for resource")
    public ResponseEntity<UpdateInformationResource> getUpdateInformationOfResource(
            @PathVariable(name = "resourceId") UUID resourceId
    ) {
        var jwtAuthenticationToken = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

        var updateInformation = this.updateManager.getUpdateInformationOfResource(resourceId, jwtAuthenticationToken);

        return ResponseEntity.ok(updateInformation);
    }

    @RequestMapping(value = "/types/{resourceTypeName}/updates", method = RequestMethod.GET)
    @Operation(summary = "Get available updates for resource")
    public ResponseEntity<UpdateInformationResourceType> getUpdateInformationOfResourceType(
            @PathVariable(name = "resourceTypeName") String resourceTypeName
    ) throws ResourceTypeNotFoundException {
        var jwtAuthenticationToken = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

        var updateInformation = this.updateManager.getUpdateInformationOfResourceType(resourceTypeName, jwtAuthenticationToken);

        return ResponseEntity.ok(updateInformation);
    }

    @RequestMapping(value = "/updates/{softwareNameplateId}/file/{fileName}", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Get update file of a software nameplate of a resource")
    public ResponseEntity<byte[]> getUpdateFileOfSoftwareNameplate(
            @PathVariable(name = "softwareNameplateId") String softwareNameplateIdBase64Encoded,
            @PathVariable(name = "fileName") String fileName
    ) throws IOException {
        var jwtAuthenticationToken = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

        var fileInputStream = this.updateManager.getUpdateFileOfSoftwareNameplate(
                softwareNameplateIdBase64Encoded,
                fileName,
                jwtAuthenticationToken
        );

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .body(IOUtils.toByteArray(fileInputStream));
    }

    @RequestMapping(value = "/updates/{softwareNameplateId}/file",
            method = RequestMethod.PUT, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary="Add or update firmware update file")
    public void addOrUpdateFirmwareUpdateFile(
            @PathVariable(name = "softwareNameplateId")  String softwareNameplateIdBase64Encoded,
            @RequestPart("file") MultipartFile firmwareUpdateFile
    ) throws MinioUploadException, MinioObjectPathNameException, MinioBucketNameException, MinioBucketCreateException, MinioRemoveObjectException {
        updateManager.addOrUpdateFirmwareUpdateFile(softwareNameplateIdBase64Encoded, firmwareUpdateFile);
    }

    @RequestMapping(value = "/updates/{softwareNameplateId}/file", method = RequestMethod.DELETE)
    @Operation(summary="Delete firmware update file")
    public void deleteFirmwareUpdateFile(
            @PathVariable(name = "softwareNameplateId")  String softwareNameplateIdBase64Encoded
    ) throws MinioObjectPathNameException, MinioBucketNameException, MinioRemoveObjectException {
        updateManager.deleteFirmwareUpdateFile(softwareNameplateIdBase64Encoded);
    }

}
