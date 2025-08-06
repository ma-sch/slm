package org.eclipse.slm.resource_management.service.rest.discovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.eclipse.slm.common.consul.model.exceptions.ConsulLoginFailedException;
import org.eclipse.slm.common.utils.keycloak.KeycloakTokenUtil;
import org.eclipse.slm.common.utils.objectmapper.ObjectMapperUtils;
import org.eclipse.slm.notification_service.messaging.NotificationMessage;
import org.eclipse.slm.notification_service.model.*;
import org.eclipse.slm.notification_service.messaging.NotificationMessageSender;
import org.eclipse.slm.resource_management.model.capabilities.CapabilityNotFoundException;
import org.eclipse.slm.resource_management.model.discovery.*;
import org.eclipse.slm.resource_management.model.resource.exceptions.ResourceNotFoundException;
import org.eclipse.slm.resource_management.persistence.api.DiscoveryJobRepository;
import org.eclipse.slm.resource_management.service.discovery.driver.discovery.DiscoveryJobListener;
import org.eclipse.slm.resource_management.service.discovery.driver.discovery.DiscoveryDriverClientFactory;
import org.eclipse.slm.resource_management.service.discovery.driver.DriverRegistryClient;
import org.eclipse.slm.resource_management.service.discovery.exceptions.DriverNotFoundException;
import org.eclipse.slm.resource_management.service.rest.resources.aas.submodels.digitalnameplate.DigitalNameplateV3;
import org.eclipse.slm.resource_management.service.rest.resources.ResourcesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class DiscoveryHandler implements DiscoveryJobListener {

    private final static Logger LOG = LoggerFactory.getLogger(DiscoveryHandler.class);

    private final DiscoveryJobRepository discoveryJobRepository;

    private final DriverRegistryClient driverRegistryClient;

    private final DiscoveryDriverClientFactory discoveryDriverClientFactory;

    private final NotificationMessageSender notificationMessageSender;

    private final Map<UUID, JwtAuthenticationToken> jobIdToTokenMap = new HashMap<>();

    private final Map<UUID, String> jobIdToDriverId = new HashMap<>();

    private final ResourcesManager resourcesManager;

    public DiscoveryHandler(DiscoveryJobRepository discoveryJobRepository,
                            DriverRegistryClient driverRegistryClient,
                            DiscoveryDriverClientFactory discoveryDriverClientFactory,
                            NotificationMessageSender notificationMessageSender, ResourcesManager resourcesManager) {
        this.discoveryJobRepository = discoveryJobRepository;
        this.driverRegistryClient = driverRegistryClient;
        this.discoveryDriverClientFactory = discoveryDriverClientFactory;
        this.notificationMessageSender = notificationMessageSender;
        this.resourcesManager = resourcesManager;
    }

    public UUID discover(JwtAuthenticationToken jwtAuthenticationToken, String driverId, DiscoveryRequest discoveryRequest)
            throws DriverNotFoundException {
        var driverInfo = this.driverRegistryClient.getRegisteredDriver(driverId);
        var driverClient = this.discoveryDriverClientFactory.createDriverClient(driverInfo);
        var discoveryJob = driverClient.discover(this, discoveryRequest);

        jobIdToTokenMap.put(discoveryJob.getId(), jwtAuthenticationToken);
        jobIdToDriverId.put(discoveryJob.getId(), driverId);

        return discoveryJob.getId();
    }

    public Collection<DiscoveredResourceDTO> getDiscoveredResources(JwtAuthenticationToken jwtAuthenticationToken,
                                                                    boolean removeDuplicate,
                                                                    boolean onlyLatestJobs,
                                                                    boolean includeIgnored,
                                                                    boolean includedOnboarded) {
        var allDiscoveryJobs = this.discoveryJobRepository.findAll();
        var discoveryJobsGroupedByDriverId = allDiscoveryJobs.stream().collect(Collectors.groupingBy(DiscoveryJob::getDriverId));

        List<DiscoveryJob> discoveryJobs;
        if (onlyLatestJobs) {
            discoveryJobs = new ArrayList<>();
            for (var discoveryJobOfDriver : discoveryJobsGroupedByDriverId.entrySet()) {
                var discoveryJobsOfDriver = discoveryJobOfDriver.getValue();
                var latestDiscoveryJob = discoveryJobsOfDriver.stream().max(Comparator.comparing(DiscoveryJob::getStartDate)).orElse(null);
                discoveryJobsOfDriver.clear();
                if (latestDiscoveryJob != null) {
                    discoveryJobs.add(latestDiscoveryJob);
                }
            }
        }
        else {
            discoveryJobs = allDiscoveryJobs;
        }


        if (removeDuplicate) {
            var discoveredResourceDTOs = new HashMap<UUID, DiscoveredResourceDTO>();

            for (var discoveryJob : discoveryJobs) {
                for (var discoveredResource : discoveryJob.getDiscoveryResult()) {
                    var discoveredResourceDTO = ObjectMapperUtils.map(discoveredResource, DiscoveredResourceDTO.class);
                    this.completeDiscoveredResourceDTO(jwtAuthenticationToken, discoveredResourceDTO, discoveryJob);
                    if ((includedOnboarded || !discoveredResourceDTO.isOnboarded())
                            && (includeIgnored || !discoveredResourceDTO.isIgnored())) {
                        discoveredResourceDTOs.put(discoveredResourceDTO.getResourceId(), discoveredResourceDTO);
                    }
                }
            }

            return discoveredResourceDTOs.values();
        }
        else {
            var discoveredResourceDTOs = new ArrayList<DiscoveredResourceDTO>();

            for (var discoveryJob : discoveryJobs) {
                var discoveredResourceDTOsOfJob = ObjectMapperUtils.mapAll(discoveryJob.getDiscoveryResult(), DiscoveredResourceDTO.class);
                for (var discoveredResourceDTO : discoveredResourceDTOsOfJob) {
                    this.completeDiscoveredResourceDTO(jwtAuthenticationToken, discoveredResourceDTO, discoveryJob);
                    if ((includedOnboarded || !discoveredResourceDTO.isOnboarded())
                            && (includeIgnored || !discoveredResourceDTO.isIgnored())) {
                        discoveredResourceDTOs.add(discoveredResourceDTO);
                    }
                }
            }

            return discoveredResourceDTOs;

        }

    }

    private void completeDiscoveredResourceDTO(JwtAuthenticationToken jwtAuthenticationToken,
                                                                DiscoveredResourceDTO discoveredResourceDTO,
                                                                DiscoveryJob discoveryJob) {
        discoveredResourceDTO.setDiscoveryJobId(discoveryJob.getId());
        discoveredResourceDTO.setResultId(discoveryJob.getId() + ":" + discoveredResourceDTO.getResourceId());

        try {
            var existingResource = resourcesManager.getResourceWithCredentialsByRemoteAccessService(jwtAuthenticationToken,
                    discoveredResourceDTO.getResourceId());
            if (existingResource != null) {
                discoveredResourceDTO.setOnboarded(true);
            }
        } catch (ConsulLoginFailedException | JsonProcessingException e) {
            LOG.error(e.getMessage());
        } catch (ResourceNotFoundException ignored) {
        }
    }

    @Override
    public void onDiscoveryCompleted(DiscoveryJob completedDiscoveryJob) {
        var jwtAuthenticationToken = jobIdToTokenMap.get(completedDiscoveryJob.getId());
        jobIdToTokenMap.remove(completedDiscoveryJob.getId());
        var driverInfo = jobIdToDriverId.get(completedDiscoveryJob.getId());
        jobIdToDriverId.remove(completedDiscoveryJob.getId());

        this.notificationMessageSender.sendMessage(new NotificationMessage(
                KeycloakTokenUtil.getUserUuid(jwtAuthenticationToken),
                NotificationCategory.RESOURCES, NotificationSubCategory.DISCOVERY, EventType.UPDATED,
                completedDiscoveryJob
        ));
    }

    public void onboard(JwtAuthenticationToken jwtAuthenticationToken, String resultId)
            throws CapabilityNotFoundException, ConsulLoginFailedException, ResourceNotFoundException, SSLException, JsonProcessingException, IllegalAccessException {
        var discoveryJobId = resultId.split(":")[0];
        var resourceIdString = resultId.split(":")[1];
        var resourceId = UUID.fromString(resourceIdString);

        var discoveryJobOptional = this.discoveryJobRepository.findById(UUID.fromString(discoveryJobId));
        if (discoveryJobOptional.isPresent()) {
            var discoveryJob = discoveryJobOptional.get();
            var discoveredResourceOptional = discoveryJob.getDiscoveryResult().stream().filter(discoveredResource -> discoveredResource.getResourceId().equals(resourceId)).findFirst();
            if (discoveredResourceOptional.isPresent()) {
                var discoveredResource = discoveredResourceOptional.get();

                var digitalNameplateV3 = new DigitalNameplateV3.Builder(
                        discoveredResource.getId(),
                        discoveredResource.getManufacturerName(),
                        discoveredResource.getProductName(),
                        "N/A"
                );
                digitalNameplateV3.firmwareVersion(discoveredResource.getFirmwareVersion());

                resourcesManager.addExistingResource(jwtAuthenticationToken,
                        discoveredResource.getResourceId(),
                        discoveredResource.getId(),
                        discoveredResource.getIpAddress(),
                        discoveredResource.getIpAddress(),
                        discoveredResource.getFirmwareVersion(),
                        discoveryJob.getDriverId(),
                        digitalNameplateV3.build());

                resourcesManager.setConnectionParametersOfResource(resourceId, discoveredResource.getConnectionParameters());
            }
        }
    }

    public void ignore(String resultId, boolean ignored) {
        String discoveryJobId = resultId.split(":")[0];
        String resourceId = resultId.split(":")[1];

        var discoveryJobOptional = this.discoveryJobRepository.findById(UUID.fromString(discoveryJobId));
        if (discoveryJobOptional.isPresent()) {
            var discoveryJob = discoveryJobOptional.get();
            var discoveredResourceOptional = discoveryJob.getDiscoveryResult().stream().filter(discoveredResource -> discoveredResource.getResourceId().equals(UUID.fromString(resourceId))).findFirst();
            if (discoveredResourceOptional.isPresent()) {
                var discoveredResource = discoveredResourceOptional.get();
                discoveredResource.setIgnored(ignored);
                this.discoveryJobRepository.save(discoveryJob);
            }
        }
    }
}
