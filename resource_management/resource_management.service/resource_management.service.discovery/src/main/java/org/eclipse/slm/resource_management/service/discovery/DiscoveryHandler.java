package org.eclipse.slm.resource_management.service.discovery;

import org.eclipse.slm.notification_service.model.Category;
import org.eclipse.slm.notification_service.model.JobGoal;
import org.eclipse.slm.notification_service.model.JobTarget;
import org.eclipse.slm.notification_service.service.client.NotificationServiceClient;
import org.eclipse.slm.resource_management.model.discovery.DiscoveredResource;
import org.eclipse.slm.resource_management.model.discovery.DiscoveryJob;
import org.eclipse.slm.resource_management.model.discovery.DriverInfo;
import org.eclipse.slm.resource_management.persistence.api.DiscoveryJobRepository;
import org.eclipse.slm.resource_management.service.discovery.driver.DiscoveryJobListener;
import org.eclipse.slm.resource_management.service.discovery.driver.DriverClientFactory;
import org.eclipse.slm.resource_management.service.discovery.driver.DriverRegistryClient;
import org.eclipse.slm.resource_management.service.discovery.exceptions.DriverNotFoundException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class DiscoveryHandler implements DiscoveryJobListener {

    private final DiscoveryJobRepository discoveryJobRepository;

    private final DriverRegistryClient driverRegistryClient;

    private final DriverClientFactory driverClientFactory;

    private final NotificationServiceClient notificationServiceClient;

    private final Map<UUID, JwtAuthenticationToken> jobIdToTokenMap = new HashMap<>();

    private final Map<UUID, DriverInfo> jobIdToDriverInfoMap = new HashMap<>();

    public DiscoveryHandler(DiscoveryJobRepository discoveryJobRepository,
                            DriverRegistryClient driverRegistryClient,
                            DriverClientFactory driverClientFactory,
                            NotificationServiceClient notificationServiceClient) {
        this.discoveryJobRepository = discoveryJobRepository;
        this.driverRegistryClient = driverRegistryClient;
        this.driverClientFactory = driverClientFactory;
        this.notificationServiceClient = notificationServiceClient;
    }

    public UUID discover(JwtAuthenticationToken jwtAuthenticationToken, String driverId) throws DriverNotFoundException {
        var driverInfo = this.driverRegistryClient.getRegisteredDriver(driverId);

        var driverClient = this.driverClientFactory.createDriverClient(driverInfo);
        var discoveryJob = driverClient.discover(this);

        jobIdToTokenMap.put(discoveryJob.getId(), jwtAuthenticationToken);
        jobIdToDriverInfoMap.put(discoveryJob.getId(), driverInfo);

        return discoveryJob.getId();
    }

    public Collection<DiscoveredResource> getDiscoveredResources(boolean removeDuplicate,
                                                                 boolean onlyLatestJobs,
                                                                 boolean includeIgnored) {

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
            var discoveredResources = new HashMap<UUID, DiscoveredResource>();
            for (var discoveryJob : discoveryJobs) {
                for (var discoveredResource : discoveryJob.getDiscoveryResult()) {
                    discoveredResources.put(discoveredResource.getResourceId(), discoveredResource);
                }
            }

            return discoveredResources.values();
        }
        else {
            var discoveredResources = new ArrayList<DiscoveredResource>();
            for (var discoveryJob : discoveryJobs) {
                discoveredResources.addAll(discoveryJob.getDiscoveryResult());
            }

            return discoveredResources;
        }
    }

    @Override
    public void onDiscoveryCompleted(DiscoveryJob completedDiscoveryJob) {
        var jwtAuthenticationToken = jobIdToTokenMap.get(completedDiscoveryJob.getId());
        jobIdToTokenMap.remove(completedDiscoveryJob.getId());
        var driverInfo = jobIdToDriverInfoMap.get(completedDiscoveryJob.getId());
        jobIdToDriverInfoMap.remove(completedDiscoveryJob.getId());

        var notificationText = "Discovery of driver " + driverInfo.getName() + " completed";

        this.notificationServiceClient.postNotification(jwtAuthenticationToken, Category.RESOURCES, JobTarget.DISCOVERY, JobGoal.ADD, notificationText);
    }
}
