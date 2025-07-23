package org.eclipse.slm.resource_management.service.rest.update;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.io.IOUtils;
import org.eclipse.digitaltwin.basyx.http.Base64UrlEncodedIdentifier;
import org.eclipse.slm.common.minio.model.exceptions.*;
import org.eclipse.slm.resource_management.model.resource.exceptions.ResourceTypeNotFoundException;
import org.eclipse.slm.resource_management.model.update.FirmwareUpdateEvents;
import org.eclipse.slm.resource_management.model.update.FirmwareUpdateJob;
import org.eclipse.slm.resource_management.model.update.UpdateInformationResource;
import org.eclipse.slm.resource_management.model.update.UpdateInformationResourceType;
import org.eclipse.slm.resource_management.persistence.api.FirmwareUpdateJobsJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import javax.ws.rs.QueryParam;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/resources")
@Tag(name = "Updates")
public class UpdatesRestController {

    private final static Logger LOG = LoggerFactory.getLogger(UpdatesRestController.class);

    private final UpdateManager updateManager;

    private final FirmwareUpdateJobsJpaRepository firmwareUpdateJobsJpaRepository;

    private final FirmwareUpdateJobFactory firmwareUpdateJobFactory;

    private final FirmwareUpdateJobStateMachineFactory firmwareUpdateJobStateMachineFactory;

    public UpdatesRestController(UpdateManager updateManager,
                                 FirmwareUpdateJobsJpaRepository firmwareUpdateJobsJpaRepository,
                                 FirmwareUpdateJobFactory firmwareUpdateJobFactory,
                                 FirmwareUpdateJobStateMachineFactory firmwareUpdateJobStateMachineFactory) {
        this.updateManager = updateManager;
        this.firmwareUpdateJobsJpaRepository = firmwareUpdateJobsJpaRepository;
        this.firmwareUpdateJobFactory = firmwareUpdateJobFactory;
        this.firmwareUpdateJobStateMachineFactory = firmwareUpdateJobStateMachineFactory;
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

    @RequestMapping(value = "/updates/{softwareNameplateId}/file/download",
            method = RequestMethod.POST)
    @Operation(summary="Download firmware update file from vendor")
    public void downloadFirmwareUpdateFileFromVendor(
            @PathVariable(name = "softwareNameplateId")  String softwareNameplateIdBase64Encoded
    ) throws MinioUploadException, MinioObjectPathNameException, MinioBucketNameException, MinioBucketCreateException, MinioRemoveObjectException {
        var jwtAuthenticationToken = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        var softwareNameplateId = Base64UrlEncodedIdentifier.fromEncodedValue(softwareNameplateIdBase64Encoded).getIdentifier();

        updateManager.downloadFirmwareUpdateFileFromVendor(softwareNameplateId, jwtAuthenticationToken);
    }

    @RequestMapping(value = "/{resourceId}/updates/jobs",
            method = RequestMethod.GET)
    @Operation(summary="Get firmware updates jobs of resource")
    public ResponseEntity<List<FirmwareUpdateJob>> getFirmwareUpdateJobsOfResource(
            @PathVariable(name = "resourceId")  UUID resourceId
    ) {
        var firmwareUpdateJobs = this.firmwareUpdateJobsJpaRepository.findByResourceId(resourceId);

        return ResponseEntity.ok(firmwareUpdateJobs);
    }

    @RequestMapping(value = "/{resourceId}/updates/jobs",
            method = RequestMethod.POST)
    @Operation(summary="Start firmware update job for a resource")
    public void prepareFirmwareUpdateOnResource(
            @PathVariable(name = "resourceId")  UUID resourceId,
            @RequestParam(name = "softwareNameplateId")  String softwareNameplateIdBase64Encoded
    ) throws Exception {
        var softwareNameplateId = Base64UrlEncodedIdentifier.fromEncodedValue(softwareNameplateIdBase64Encoded).getIdentifier();

        this.firmwareUpdateJobFactory.create(resourceId, softwareNameplateId);
    }

    @RequestMapping(value = "/{resourceId}/updates/jobs/{firmwareUpdateJobId}",
            method = RequestMethod.POST)
    @Operation(summary="Prepare firmware update on resource")
    public void prepareFirmwareUpdateOnResource(
            @PathVariable(name = "resourceId")  UUID resourceId,
            @PathVariable(name = "firmwareUpdateJobId")  UUID firmwareUpdateJobId,
            @RequestParam(name = "event") FirmwareUpdateEvents event
            ) throws Exception {
        var firmwareUpdateJobStateMachine = firmwareUpdateJobStateMachineFactory.create(firmwareUpdateJobId);

        Message<FirmwareUpdateEvents> message = MessageBuilder.withPayload(event)
                        .setHeader("resourceId", resourceId)
                        .setHeader("firmwareUpdateJobId", firmwareUpdateJobId)
                        .build();

        firmwareUpdateJobStateMachine.sendEvent(Mono.just(message)).blockFirst();
    }


}
